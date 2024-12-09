/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.quarkus.runtime.configuration.mappers;

import static java.util.Optional.ofNullable;
import static org.keycloak.quarkus.runtime.Environment.isRebuild;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR_CHAR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toCliFormat;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toEnvVarFormat;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.ConfigValue.ConfigValueBuilder;
import io.smallrye.config.ExpressionConfigSourceInterceptor;
import io.smallrye.config.Expressions;
import org.keycloak.config.DeprecatedMetadata;
import org.keycloak.config.Option;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.cli.ShortErrorMessageHandler;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.utils.StringUtil;

public class PropertyMapperImpl<T> implements PropertyMapper<T> {

    protected final Option<T> option;
    private final String to;
    private BooleanSupplier enabled;
    private String enabledWhen;
    private final BiFunction<String, ConfigSourceInterceptorContext, String> mapper;
    private final String mapFrom;
    private final BiFunction<String, ConfigSourceInterceptorContext, String> parentMapper;
    private final boolean mask;
    private final String paramLabel;
    private final String envVarFormat;
    private final String cliFormat;
    private final BiConsumer<PropertyMapper<T>, ConfigValue> validator;
    private final String description;
    private final BooleanSupplier required;
    private final String requiredWhen;

    PropertyMapperImpl(Option<T> option, String to, BooleanSupplier enabled, String enabledWhen,
                       BiFunction<String, ConfigSourceInterceptorContext, String> mapper,
                       String mapFrom, BiFunction<String, ConfigSourceInterceptorContext, String> parentMapper,
                       String paramLabel, boolean mask, BiConsumer<PropertyMapper<T>, ConfigValue> validator,
                       String description, BooleanSupplier required, String requiredWhen, String from) {
        this.option = option;
        this.to = to == null ? getFrom() : to;
        this.enabled = enabled;
        this.enabledWhen = enabledWhen;
        this.mapper = mapper;
        this.mapFrom = mapFrom;
        this.paramLabel = paramLabel;
        this.mask = mask;
        this.cliFormat = toCliFormat(option.getKey());
        this.required = required;
        this.requiredWhen = requiredWhen;
        this.envVarFormat = toEnvVarFormat(getFrom());
        this.validator = validator;
        this.description = description;
        this.parentMapper = parentMapper;
    }

    @Override
    public ConfigValue getConfigValue(ConfigSourceInterceptorContext context) {
        return getConfigValue(to, context);
    }

    @Override
    public ConfigValue getConfigValue(String name, ConfigSourceInterceptorContext context) {
        String from = getFrom();

        if (to != null && to.endsWith(OPTION_PART_SEPARATOR)) {
            // in case mapping is based on prefixes instead of full property names
            from = name.replace(to.substring(0, to.lastIndexOf('.')), from.substring(0, from.lastIndexOf(OPTION_PART_SEPARATOR_CHAR)));
        }

        if ((isRebuild() || Environment.isRebuildCheck()) && isRunTime()) {
            // during re-aug do not resolve the server runtime properties and avoid they included by quarkus in the default value config source
            return ConfigValue.builder().withName(name).build();
        }

        // try to obtain the value for the property we want to map first
        ConfigValue config = convertValue(context.proceed(from));

        boolean parentValue = false;
        if (mapFrom != null && (config == null || config.getValue() == null)) {
            // if the property we want to map depends on another one, we use the value from the other property to call the mapper
            config = Configuration.getKcConfigValue(mapFrom);
            parentValue = true;
        }

        if (config != null && config.getValue() != null) {
            config = transformValue(name, config, context, parentValue);
        } else {
            String defaultValue = this.option.getDefaultValue().map(Option::getDefaultValueString).orElse(null);
            config = transformValue(name, new ConfigValueBuilder().withName(name)
                    .withValue(defaultValue).withRawValue(defaultValue).build(),
                    context, false);
        }

        if (config != null) {
            return config;
        }

        // now try any defaults from quarkus
        return context.proceed(name);
    }

    @Override
    public Option<T> getOption() {
        return this.option;
    }

    @Override
    public void setEnabled(BooleanSupplier enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled.getAsBoolean();
    }

    @Override
    public Optional<String> getEnabledWhen() {
        return Optional.of(enabledWhen)
                .filter(StringUtil::isNotBlank)
                .map(e -> "Available only when " + e);
    }

    @Override
    public void setEnabledWhen(String enabledWhen) {
        this.enabledWhen = enabledWhen;
    }

    @Override
    public boolean isRequired() {
        return required.getAsBoolean();
    }

    @Override
    public Optional<String> getRequiredWhen() {
        return Optional.of(requiredWhen)
                .filter(StringUtil::isNotBlank)
                .map(e -> "Required when " + e);
    }

    @Override
    public Class<T> getType() {
        return this.option.getType();
    }

    @Override
    public String getFrom() {
        return MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + this.option.getKey();
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    /**
     * If {@link #isStrictExpectedValues()} is false, custom values can be provided
     * Otherwise, only specified expected values can be used.
     *
     * @return expected values
     */
    @Override
    public List<String> getExpectedValues() {
        return this.option.getExpectedValues();
    }

    @Override
    public boolean isStrictExpectedValues() {
        return this.option.isStrictExpectedValues();
    }

    @Override
    public Optional<T> getDefaultValue() { return this.option.getDefaultValue(); }

    @Override
    public OptionCategory getCategory() {
        return this.option.getCategory();
    }

    @Override
    public boolean isHidden() { return this.option.isHidden(); }

    @Override
    public boolean isBuildTime() {
        return this.option.isBuildTime();
    }

    @Override
    public boolean isRunTime() {
        return !this.option.isBuildTime();
    }

    @Override
    public String getTo() {
        return to;
    }

    @Override
    public String getParamLabel() {
        return paramLabel;
    }

    @Override
    public String getCliFormat() {
        return cliFormat;
    }

    @Override
    public String getEnvVarFormat() {
        return envVarFormat;
    }

    @Override
    public boolean isMask() {
        return mask;
    }

    @Override
    public Optional<DeprecatedMetadata> getDeprecatedMetadata() {
        return option.getDeprecatedMetadata();
    }

    /**
     * An option is considered a wildcard option if its key contains a wildcard placeholder (e.g. log-level-<category>).
     * The placeholder must be denoted by the '<' and '>' characters.
     */
    @Override
    public boolean hasWildcard() {
        return false;
    }

    private ConfigValue transformValue(String name, ConfigValue configValue, ConfigSourceInterceptorContext context, boolean parentValue) {
        String value = configValue.getValue();
        String mappedValue = value;

        boolean mapped = false;
        var theMapper = parentValue ? this.parentMapper : this.mapper;
        if (theMapper != null && (!name.equals(getFrom()) || parentValue)) {
            mappedValue = theMapper.apply(value, context);
            mapped = true;
        }

        // defaults and values from transformers may not have been subject to expansion
        if ((mapped || configValue.getConfigSourceName() == null) && mappedValue != null && Expressions.isEnabled() && mappedValue.contains("$")) {
            mappedValue = new ExpressionConfigSourceInterceptor().getValue(
                    new ContextWrapper(context, new ConfigValueBuilder().withName(name).withValue(mappedValue).build()),
                    name).getValue();
        }

        if (value == null && mappedValue == null) {
            return null;
        }

        if (!mapped && name.equals(configValue.getName())) {
            return configValue;
        }

        // by unsetting the ordinal this will not be seen as directly modified by the user
        return configValue.from().withName(name).withValue(mappedValue).withRawValue(value).withConfigSourceOrdinal(0).build();
    }

    private ConfigValue convertValue(ConfigValue configValue) {
        if (configValue == null) {
            return null;
        }

        return configValue.withValue(ofNullable(configValue.getValue()).map(String::trim).orElse(null));
    }

    private final class ContextWrapper implements ConfigSourceInterceptorContext {
        private final ConfigSourceInterceptorContext context;
        private final ConfigValue value;

        private ContextWrapper(ConfigSourceInterceptorContext context, ConfigValue value) {
            this.context = context;
            this.value = value;
        }

        @Override
        public ConfigValue restart(String name) {
            return context.restart(name);
        }

        @Override
        public ConfigValue proceed(String name) {
            if (name.equals(value.getName())) {
                return value;
            }
            return context.proceed(name);
        }

        @Override
        public Iterator<String> iterateNames() {
            return context.iterateNames();
        }
    }

    @Override
    public void validate(ConfigValue value) {
        if (validator != null) {
            validator.accept(this, value);
        }
    }

    @Override
    public boolean isList() {
        return getOption().getType() == java.util.List.class;
    }

    @Override
    public void validateValues(ConfigValue configValue, BiConsumer<ConfigValue, String> singleValidator) {
        String value = configValue.getValue();

        boolean multiValued = isList();
        StringBuilder result = new StringBuilder();

        String[] values = multiValued ? value.split(",") : new String[] { value };
        for (String v : values) {
            if (multiValued && !v.trim().equals(v)) {
                if (!result.isEmpty()) {
                    result.append(".\n");
                }
                result.append("Invalid value for multivalued option ")
                        .append(getOptionAndSourceMessage(configValue))
                        .append(": list value '")
                        .append(v)
                        .append("' should not have leading nor trailing whitespace");
                continue;
            }
            try {
                singleValidator.accept(configValue, v);
            } catch (PropertyException e) {
                if (!result.isEmpty()) {
                    result.append(".\n");
                }
                result.append(e.getMessage());
            }
        }

        if (!result.isEmpty()) {
            throw new PropertyException(result.toString());
        }
    }

    @Override
    public void validateExpectedValues(ConfigValue configValue, String v) {
        List<String> expectedValues = getExpectedValues();
        if (!expectedValues.isEmpty() && getOption().isStrictExpectedValues() && !expectedValues.contains(v)
                && (!getOption().isCaseInsensitiveExpectedValues()
                        || !expectedValues.stream().anyMatch(v::equalsIgnoreCase))) {
            throw new PropertyException(
                    String.format("Invalid value for option %s: %s.%s", getOptionAndSourceMessage(configValue), v,
                            ShortErrorMessageHandler.getExpectedValuesMessage(expectedValues, getOption().isCaseInsensitiveExpectedValues())));
        }
    }

    @Override
    public String getOptionAndSourceMessage(ConfigValue configValue) {
        if (PropertyMapper.isCliOption(configValue)) {
            return String.format("'%s'", this.getCliFormat());
        }
        if (PropertyMapper.isEnvOption(configValue)) {
            return String.format("'%s'", this.getEnvVarFormat());
        }
        return String.format("'%s' in %s", getFrom(),
                KeycloakConfigSourceProvider.getConfigSourceDisplayName(configValue.getConfigSourceName()));
    }

    /**
     * Get all Keycloak config values for the mapper. A multivalued config option is a config option that
     * has a wildcard in its name, e.g. log-level-<category>.
     *
     * @return a list of config values where the key is the resolved wildcard (e.g. category) and the value is the config value
     */
    @Override
    public List<ConfigValue> getKcConfigValues() {
        return List.of(Configuration.getConfigValue(getFrom()));
    }

}
