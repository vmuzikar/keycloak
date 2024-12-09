/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.DeprecatedMetadata;
import org.keycloak.config.Option;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.KcEnvConfigSource;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.keycloak.config.Option.WILDCARD_PLACEHOLDER_PATTERN;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface PropertyMapper<T> {
    static <T> Builder<T> fromOption(Option<T> opt) {
        return new Builder<>(opt);
    }

    static boolean isCliOption(ConfigValue configValue) {
        return Optional.ofNullable(configValue.getConfigSourceName()).filter(name -> name.contains(ConfigArgsConfigSource.NAME)).isPresent();
    }

    static boolean isEnvOption(ConfigValue configValue) {
        return Optional.ofNullable(configValue.getConfigSourceName()).filter(name -> name.contains(KcEnvConfigSource.NAME)).isPresent();
    }

    ConfigValue getConfigValue(ConfigSourceInterceptorContext context);

    ConfigValue getConfigValue(String name, ConfigSourceInterceptorContext context);

    Option<T> getOption();

    void setEnabled(BooleanSupplier enabled);

    boolean isEnabled();

    Optional<String> getEnabledWhen();

    void setEnabledWhen(String enabledWhen);

    boolean isRequired();

    Optional<String> getRequiredWhen();

    Class<T> getType();

    String getFrom();

    String getDescription();

    List<String> getExpectedValues();

    boolean isStrictExpectedValues();

    Optional<T> getDefaultValue();

    OptionCategory getCategory();

    boolean isHidden();

    boolean isBuildTime();

    boolean isRunTime();

    String getTo();

    String getParamLabel();

    String getCliFormat();

    String getEnvVarFormat();

    boolean isMask();

    Optional<DeprecatedMetadata> getDeprecatedMetadata();

    boolean hasWildcard();

    void validate(ConfigValue value);

    boolean isList();

    void validateValues(ConfigValue configValue, BiConsumer<ConfigValue, String> singleValidator);

    void validateExpectedValues(ConfigValue configValue, String v);

    String getOptionAndSourceMessage(ConfigValue configValue);

    List<ConfigValue> getKcConfigValues();

    @FunctionalInterface
    interface ValueMapper {
        String map(String name, String value, ConfigSourceInterceptorContext context);
    }

    class Builder<T> {

        private final Option<T> option;
        private String to;
        private BiFunction<String, ConfigSourceInterceptorContext, String> mapper;
        private String mapFrom = null;
        private BiFunction<String, ConfigSourceInterceptorContext, String> parentMapper;
        private boolean isMasked = false;
        private BooleanSupplier isEnabled = () -> true;
        private String enabledWhen = "";
        private String paramLabel;
        private BiConsumer<PropertyMapper<T>, ConfigValue> validator = (mapper, value) -> mapper.validateValues(value, mapper::validateExpectedValues);
        private String description;
        private BooleanSupplier isRequired = () -> false;
        private String requiredWhen = "";
        private Function<Set<String>, Set<String>> wildcardKeysTransformer;
        private ValueMapper wildcardMapFrom;

        public Builder(Option<T> option) {
            this.option = option;
            this.description = this.option.getDescription();
        }

        public Builder<T> to(String to) {
            this.to = to;
            return this;
        }

        /**
         * NOTE: This transformer will not apply to the mapFrom value. When using
         * {@link #mapFrom} you generally need a transformer specifically for the parent
         * value, see {@link #mapFrom(Option, BiFunction)}
         * <p>
         * The value passed into the transformer may be null if the property has no value set, and no default
         */
        public Builder<T> transformer(BiFunction<String, ConfigSourceInterceptorContext, String> mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder<T> paramLabel(String label) {
            this.paramLabel = label;
            return this;
        }

        public Builder<T> mapFrom(Option<?> mapFrom) {
            this.mapFrom = mapFrom.getKey();
            return this;
        }

        public Builder<T> mapFrom(Option<?> mapFrom, BiFunction<String, ConfigSourceInterceptorContext, String> parentMapper) {
            this.mapFrom = mapFrom.getKey();
            this.parentMapper = parentMapper;
            return this;
        }

        public Builder<T> isMasked(boolean isMasked) {
            this.isMasked = isMasked;
            return this;
        }

        public Builder<T> isEnabled(BooleanSupplier isEnabled, String enabledWhen) {
            this.isEnabled = isEnabled;
            this.enabledWhen = enabledWhen;
            return this;
        }

        public Builder<T> isEnabled(BooleanSupplier isEnabled) {
            this.isEnabled = isEnabled;
            return this;
        }

        /**
         * Sets this option as required when the {@link BooleanSupplier} returns {@code true}.
         * <p>
         * The {@code enableWhen} parameter is a message to show with the error message.
         * <p>
         * This check is only run in runtime mode.
         */
        public Builder<T> isRequired(BooleanSupplier isRequired, String requiredWhen) {
            this.requiredWhen = Objects.requireNonNull(requiredWhen);
            assert !requiredWhen.endsWith(".");
            return isRequired(isRequired);
        }

        /**
         * Sets this option as required when the {@link BooleanSupplier} returns {@code true}.
         * <p>
         * This check is only run in runtime mode.
         */
        public Builder<T> isRequired(BooleanSupplier isRequired) {
            this.isRequired = Objects.requireNonNull(isRequired);
            return this;
        }

        /**
         * Set the validator, overwriting the current one.
         */
        public Builder<T> validator(Consumer<String> validator) {
            this.validator = (mapper, value) -> mapper.validateValues(value,
                    (c, v) -> validator.accept(v));
            if (!Objects.equals(this.description, this.option.getDescription())) {
                throw new AssertionError("Overwriting the validator will cause the description modification from addValidateEnabled to be incorrect.");
            }
            return this;
        }

        public Builder<T> addValidator(BiConsumer<PropertyMapper<T>, ConfigValue> validator) {
            var current = this.validator;
            this.validator = (mapper, value) -> {
                Stream.of(current, validator).map(v -> {
                            try {
                                v.accept(mapper, value);
                                return Optional.<PropertyException>empty();
                            } catch (PropertyException e) {
                                return Optional.of(e);
                            }
                        }).flatMap(Optional::stream)
                        .reduce((e1, e2) -> new PropertyException(String.format("%s.\n%s", e1.getMessage(), e2.getMessage())))
                        .ifPresent(e -> {
                            throw e;
                        });
            };
            return this;
        }

        /**
         * Similar to {@link #enabledWhen}, but uses the condition as a validator that is added to the current one. This allows the option
         * to appear in help.
         *
         * @return
         */
        public Builder<T> addValidateEnabled(BooleanSupplier isEnabled, String enabledWhen) {
            this.addValidator((mapper, value) -> {
                if (!isEnabled.getAsBoolean()) {
                    throw new PropertyException(mapper.getOption().getKey() + " available only when " + enabledWhen);
                }
            });
            this.description = String.format("%s Available only when %s.", this.description, enabledWhen);
            return this;
        }

        public Builder<T> wildcardKeysTransformer(Function<Set<String>, Set<String>> wildcardValuesTransformer) {
            this.wildcardKeysTransformer = wildcardValuesTransformer;
            return this;
        }

        public Builder<T> wildcardMapFrom(Option<?> mapFrom, ValueMapper function) {
            this.mapFrom = mapFrom.getKey();
            this.wildcardMapFrom = function;
            return this;
        }

        public PropertyMapper<T> build() {
            if (paramLabel == null && Boolean.class.equals(option.getType())) {
                paramLabel = Boolean.TRUE + "|" + Boolean.FALSE;
            }
            // The wildcard pattern (e.g. log-level-<category>) is matching only a-z, 0-0 and dots. For env vars, dots are replaced by underscores.
            var fromWildcardMatcher = WILDCARD_PLACEHOLDER_PATTERN.matcher(option.getKey());
            if (fromWildcardMatcher.find()) {
                return new WildcardPropertyMapper<>(option, to, isEnabled, enabledWhen, mapper, mapFrom, parentMapper, paramLabel, isMasked, validator, description, isRequired, requiredWhen, fromWildcardMatcher, wildcardKeysTransformer, wildcardMapFrom);
            }
            if (wildcardKeysTransformer != null || wildcardMapFrom != null) {
                throw new AssertionError("wildcardKeysTransformer not expected with non-wildcard mapper");
            }
            return new PropertyMapperImpl<>(option, to, isEnabled, enabledWhen, mapper, mapFrom, parentMapper, paramLabel, isMasked, validator, description, isRequired, requiredWhen, null);
        }
    }
}
