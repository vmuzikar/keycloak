package org.keycloak.quarkus.runtime.configuration.mappers;

import static org.keycloak.config.Option.WILDCARD_PLACEHOLDER_PATTERN;
import static org.keycloak.quarkus.runtime.cli.Picocli.ARG_PREFIX;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.config.DeprecatedMetadata;
import org.keycloak.config.Option;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

public class WildcardPropertyMapper<T> extends PropertyMapperImpl<T> {

    private Matcher fromWildcardMatcher;
    private Pattern fromWildcardPattern;
    private Pattern envVarNameWildcardPattern;
    private Matcher toWildcardMatcher;
    private Pattern toWildcardPattern;
    private Function<Set<String>, Set<String>> wildcardKeysTransformer;
    private ValueMapper wildcardMapFrom;

    public WildcardPropertyMapper(Option<T> option, String to, BooleanSupplier enabled, String enabledWhen,
            BiFunction<String, ConfigSourceInterceptorContext, String> mapper,
            String mapFrom, BiFunction<String, ConfigSourceInterceptorContext, String> parentMapper,
            String paramLabel, boolean mask, BiConsumer<PropertyMapper<T>, ConfigValue> validator,
            String description, BooleanSupplier required, String requiredWhen, Matcher fromWildcardMatcher, Function<Set<String>, Set<String>> wildcardKeysTransformer, ValueMapper wildcardMapFrom) {
        super(option, to, enabled, enabledWhen, mapper, mapFrom, parentMapper, paramLabel, mask, validator, description, required, requiredWhen, null);
        this.wildcardMapFrom = wildcardMapFrom;
        this.fromWildcardMatcher = fromWildcardMatcher;
        // Includes handling for both "--" prefix for CLI options and "kc." prefix
        this.fromWildcardPattern = Pattern.compile("(?:" + ARG_PREFIX + "|kc\\.)" + fromWildcardMatcher.replaceFirst("([\\\\\\\\.a-zA-Z0-9]+)"));

        // Not using toEnvVarFormat because it would process the whole string incl the <...> wildcard.
        Matcher envVarMatcher = WILDCARD_PLACEHOLDER_PATTERN.matcher(option.getKey().toUpperCase().replace("-", "_"));
        this.envVarNameWildcardPattern = Pattern.compile("KC_" + envVarMatcher.replaceFirst("([_A-Z0-9]+)"));

        if (to != null) {
            toWildcardMatcher = WILDCARD_PLACEHOLDER_PATTERN.matcher(to);
            if (!toWildcardMatcher.find()) {
                throw new IllegalArgumentException("Attempted to map a wildcard option to a non-wildcard option");
            }

            this.toWildcardPattern = Pattern.compile(toWildcardMatcher.replaceFirst("([\\\\\\\\.a-zA-Z0-9]+)"));
        }

        this.wildcardKeysTransformer = wildcardKeysTransformer;
    }

    @Override
    public boolean hasWildcard() {
        return true;
    }

    private String getTo(String wildcardKey) {
        return toWildcardMatcher.replaceFirst(wildcardKey);
    }

    private String getFrom(String wildcardKey) {
        return MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + fromWildcardMatcher.replaceFirst(wildcardKey);
    }

    @Override
    public List<ConfigValue> getKcConfigValues() {
        return this.getWildcardKeys().stream().map(v -> Configuration.getConfigValue(getFrom(v))).toList();
    }

    public Set<String> getWildcardKeys() {
        // this is not optimal
        // TODO find an efficient way to get all values that match the wildcard
        Set<String> values = StreamSupport.stream(Configuration.getPropertyNames().spliterator(), false)
                .map(n -> getMappedKey(n, false))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        if (wildcardKeysTransformer != null) {
            return wildcardKeysTransformer.apply(values);
        }

        return values;
    }

    /**
     * Returns a mapped key for the given option name if a relevant mapping is available, or empty otherwise.
     * Currently, it only attempts to extract the wildcard key from the given option name.
     * E.g. for the option "log-level-<category>" and the option name "log-level-io.quarkus",
     * the wildcard value would be "io.quarkus".
     */
    private Optional<String> getMappedKey(String originalKey, boolean tryTo) {
        Matcher matcher = fromWildcardPattern.matcher(originalKey);
        if (matcher.matches()) {
            return Optional.of(matcher.group(1));
        }

        if (tryTo && toWildcardPattern != null) {
            matcher = toWildcardPattern.matcher(originalKey);
            if (matcher.matches()) {
                return Optional.of(matcher.group(1));
            }
        }

        return Optional.empty();
    }

    public Set<String> getToWithWildcards() {
        if (toWildcardMatcher == null) {
            return Set.of();
        }

        return getWildcardKeys().stream()
                .map(v -> toWildcardMatcher.replaceFirst(v))
                .collect(Collectors.toSet());
    }

    /**
     * Checks if the given option name matches the wildcard pattern of this option.
     * E.g. check if "log-level-io.quarkus" matches the wildcard pattern "log-level-<category>".
     */
    public boolean matchesWildcardOptionName(String name) {
        return fromWildcardPattern.matcher(name).matches() || envVarNameWildcardPattern.matcher(name).matches()
                || (toWildcardPattern != null && toWildcardPattern.matcher(name).matches());
    }

    public static class KeyAwareWildcardMapper implements PropertyMapper {
        private final String wildcardKey;
        private final WildcardPropertyMapper<?> delegate;

        public KeyAwareWildcardMapper(String key, boolean keyIsEnvVar, WildcardPropertyMapper<?> delegate) {
            if (!keyIsEnvVar) {
                wildcardKey = delegate.getMappedKey(key, true).orElseThrow();
            } else {
                Matcher matcher = delegate.envVarNameWildcardPattern.matcher(key);
                String value = matcher.group(1);
                wildcardKey = value.toLowerCase().replace("_", "."); // we opiniotatedly convert env var names to CLI format with dots
            }

            this.delegate = delegate;
        }

        @Override
        public ConfigValue getConfigValue(ConfigSourceInterceptorContext context) {
            return delegate.getConfigValue(context);
        }

        @Override
        public ConfigValue getConfigValue(String name, ConfigSourceInterceptorContext context) {
            return delegate.getConfigValue(name, context);
        }

        @Override
        public Option<?> getOption() {
            return delegate.getOption();
        }

        @Override
        public void setEnabled(BooleanSupplier enabled) {
            delegate.setEnabled(enabled);
        }

        @Override
        public boolean isEnabled() {
            return delegate.isEnabled();
        }

        @Override
        public Optional<String> getEnabledWhen() {
            return delegate.getEnabledWhen();
        }

        @Override
        public void setEnabledWhen(String enabledWhen) {
            delegate.setEnabledWhen(enabledWhen);
        }

        @Override
        public boolean isRequired() {
            return delegate.isRequired();
        }

        @Override
        public Optional<String> getRequiredWhen() {
            return delegate.getRequiredWhen();
        }

        @Override
        public Class<?> getType() {
            return delegate.getType();
        }

        @Override
        public String getFrom() {
            return delegate.getFrom(wildcardKey);
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public List<String> getExpectedValues() {
            return delegate.getExpectedValues();
        }

        @Override
        public boolean isStrictExpectedValues() {
            return delegate.isStrictExpectedValues();
        }

        @Override
        public Optional<?> getDefaultValue() {
            return delegate.getDefaultValue();
        }

        @Override
        public OptionCategory getCategory() {
            return delegate.getCategory();
        }

        @Override
        public boolean isHidden() {
            return delegate.isHidden();
        }

        @Override
        public boolean isBuildTime() {
            return delegate.isBuildTime();
        }

        @Override
        public boolean isRunTime() {
            return delegate.isRunTime();
        }

        @Override
        public String getTo() {
            return delegate.getTo(wildcardKey);
        }

        @Override
        public String getParamLabel() {
            return delegate.getParamLabel();
        }

        @Override
        public String getCliFormat() {
            return delegate.getCliFormat();
        }

        @Override
        public String getEnvVarFormat() {
            return delegate.getEnvVarFormat();
        }

        @Override
        public boolean isMask() {
            return delegate.isMask();
        }

        @Override
        public Optional<DeprecatedMetadata> getDeprecatedMetadata() {
            return delegate.getDeprecatedMetadata();
        }

        @Override
        public boolean hasWildcard() {
            return delegate.hasWildcard();
        }

        @Override
        public void validate(ConfigValue value) {
            delegate.validate(value);
        }

        @Override
        public boolean isList() {
            return delegate.isList();
        }

        @Override
        public void validateValues(ConfigValue configValue, BiConsumer<ConfigValue, String> singleValidator) {
            delegate.validateValues(configValue, singleValidator);
        }

        @Override
        public void validateExpectedValues(ConfigValue configValue, String v) {
            delegate.validateExpectedValues(configValue, v);
        }

        @Override
        public String getOptionAndSourceMessage(ConfigValue configValue) {
            return delegate.getOptionAndSourceMessage(configValue);
        }

        @Override
        public List<ConfigValue> getKcConfigValues() {
            return delegate.getKcConfigValues();
        }
    }

}
