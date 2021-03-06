package com.direwolf20.core.properties;

import com.direwolf20.core.DireCore20;
import com.google.common.base.Preconditions;
import net.minecraft.nbt.CompoundNBT;

import java.util.*;

/**
 * The default {@link IPropertyContainer} which is created using a {@link Builder} to add the {@link Property Properties}
 * represented by this container.
 * <p>
 * Notice that it does accept null values and serializers must handle this somehow!
 *
 * @see IPropertyContainer
 * @see Property
 */
public final class PropertyContainer implements IPropertyContainer {
    private final Map<Property<?>, Object> properties;
    private final Map<String, Property<?>> propertyByName;
    private final Set<MutableProperty<?>> mutableProperties;
    
    private PropertyContainer(Map<Property<?>, Object> properties, Map<String, Property<?>> propertyByName, Set<MutableProperty<?>> mutableProperties) {
        this.properties = properties;
        this.propertyByName = propertyByName;
        this.mutableProperties = mutableProperties;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> Optional<T> getProperty(Property<T> property) {
        return Optional.ofNullable(properties.get(property))
                .map(property::cast);
    }

    @Override
    public <T> boolean setProperty(MutableProperty<T> property, T value) {
        if (mutableProperties.contains(property)) {
            properties.put(property.getProperty(), value);
            return true;
        }
        return false;
    }

    @Override
    public Set<Property<?>> listProperties() {
        return Collections.unmodifiableSet(properties.keySet());
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        for (Map.Entry<Property<?>, Object> entry : properties.entrySet())
            nbt.put(entry.getKey().getName(), entry.getKey().serializeValue(entry.getValue()));
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        for (String key : nbt.keySet()) {
            Property<?> prop = propertyByName.get(key);
            if (prop != null) //This implicitly also checks whether the property is already in here...
                properties.put(prop, prop.deserialize(nbt.get(key)));
            else
                DireCore20.LOG.warn("Attempted to deserialize unknown Property {}. This might just be a version difference - or a bug.", key);
        }
    }

    /**
     * A simple build for the {@link PropertyContainer}. Notice that it enforces the container to only contain properties with
     * distinc {@link Property#getName() names}, as per contract of {@link IPropertyContainer}!
     */
    public static final class Builder {
        private Map<Property<?>, Object> properties;
        private Map<String, Property<?>> propertyByName;
        //this is needed to allow the builder to degrade mutable properties to immutable ones
        private Map<Property<?>, MutableProperty<?>> propertyToMutableIndex;
        private Set<MutableProperty<?>> mutableProperties;

        public Builder() {
            this.properties = new IdentityHashMap<>();
            this.propertyByName = new HashMap<>();
            this.mutableProperties = Collections.newSetFromMap(new IdentityHashMap<>());
            this.propertyToMutableIndex = new IdentityHashMap<>();
        }

        /**
         * Sets the given Property to the given value / adds it with the given default, if not present.
         *
         * @param prop  The Property to set/add
         * @param value The value to set it to
         * @param <T>   The type of the value
         * @return The Builder instance
         * @throws IllegalArgumentException if a different Property, with the same name, was already in this Builder. (See the contract of {@link IPropertyContainer}.)
         */
        public <T> Builder putProperty(Property<T> prop, T value) {
            Preconditions.checkArgument(!propertyByName.containsKey(prop.getName()) || propertyByName.get(prop.getName()) == prop,
                    "Caught ambiguous %s during PropertyContainer-Construction. Each Property in the container must have a unique serialisation name! This will not be added to the container!"
                    , prop);
            properties.put(prop, value);
            propertyByName.put(prop.getName(), prop);
            //make sure that if we just replaced some mutable property with something immutable, it will no longer be mutable
            MutableProperty<?> mutable = propertyToMutableIndex.remove(prop);
            if (mutable != null)
                mutableProperties.remove(mutable);

            return this;
        }

        public <T> Builder putProperty(MutableProperty<T> prop, T value) {
            putProperty(prop.getProperty(), value);
            //mark it as mutable
            mutableProperties.add(prop);
            propertyToMutableIndex.put(prop.getProperty(), prop);
            return this;
        }

        public PropertyContainer build() {
            //do not convert to ImmutableMap here, as string key's are not well-behaved and this would therefore be slower
            return new PropertyContainer(properties, propertyByName, mutableProperties);
        }
    }
}
