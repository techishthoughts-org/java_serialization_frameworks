package org.techishthoughts.kryo.config;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.techishthoughts.payload.model.Address;
import org.techishthoughts.payload.model.Education;
import org.techishthoughts.payload.model.Language;
import org.techishthoughts.payload.model.Order;
import org.techishthoughts.payload.model.OrderItem;
import org.techishthoughts.payload.model.Payment;
import org.techishthoughts.payload.model.Skill;
import org.techishthoughts.payload.model.SocialConnection;
import org.techishthoughts.payload.model.TrackingEvent;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.model.UserProfile;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

@Configuration
public class KryoConfiguration {

    @Bean
    @Primary
    public Kryo kryo() {
        Kryo kryo = new Kryo();

        // Optimize for performance
        kryo.setRegistrationRequired(false);
        kryo.setReferences(true);
        kryo.setAutoReset(false);

        // Register our model classes for better performance
        kryo.register(User.class);
        kryo.register(UserProfile.class);
        kryo.register(Address.class);
        kryo.register(Order.class);
        kryo.register(OrderItem.class);
        kryo.register(Payment.class);
        kryo.register(TrackingEvent.class);
        kryo.register(SocialConnection.class);
        kryo.register(Skill.class);
        kryo.register(Education.class);
        kryo.register(Language.class);

        // Register Java time classes
        kryo.register(LocalDateTime.class);

        // Register collections
        kryo.register(java.util.ArrayList.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(java.util.LinkedHashMap.class);

        // Register enums
        kryo.register(Address.AddressType.class);
        kryo.register(Order.OrderStatus.class);
        kryo.register(Payment.PaymentMethod.class);
        kryo.register(Payment.PaymentStatus.class);
        kryo.register(SocialConnection.SocialPlatform.class);
        kryo.register(Skill.SkillLevel.class);
        kryo.register(Language.LanguageProficiency.class);

        return kryo;
    }

    @Bean("optimizedKryo")
    public Kryo optimizedKryo() {
        Kryo kryo = new Kryo();

        // Maximum performance settings
        kryo.setRegistrationRequired(false);
        kryo.setReferences(false); // Disable references for maximum speed
        kryo.setAutoReset(false);
        kryo.setCopyReferences(false);

        // Use standard instantiator strategy
        kryo.setInstantiatorStrategy(new org.objenesis.strategy.StdInstantiatorStrategy());

        // Register classes
        kryo.register(User.class);
        kryo.register(UserProfile.class);
        kryo.register(Address.class);
        kryo.register(Order.class);
        kryo.register(OrderItem.class);
        kryo.register(Payment.class);
        kryo.register(TrackingEvent.class);
        kryo.register(SocialConnection.class);
        kryo.register(Skill.class);
        kryo.register(Education.class);
        kryo.register(Language.class);

        // Register Java time classes
        kryo.register(LocalDateTime.class);

        return kryo;
    }

    @Bean
    public Output output() {
        return new Output(8192, 1024 * 1024); // 8KB initial, 1MB max
    }

    @Bean
    public Input input() {
        return new Input(8192); // 8KB buffer
    }
}
