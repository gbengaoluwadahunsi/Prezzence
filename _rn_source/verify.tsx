import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React, { useEffect } from 'react';
import { StyleSheet, Text, View, Pressable, Dimensions } from 'react-native';
import Animated, {
    FadeIn,
    FadeInDown,
    ZoomIn,
    useAnimatedStyle,
    withRepeat,
    withTiming,
    withSequence,
    useSharedValue,
} from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { LinearGradient } from 'expo-linear-gradient';
import { useTranslation } from '../src/hooks/useTranslation';
import { colors, spacing, typography, radii } from '../src/theme';
import { useAuthStore } from '../src/store/authStore';
import { useSettingsStore } from '../src/store/settingsStore';
import { useNavigationStore } from '../src/store/navigationStore';
import { BrandWordmark } from '../src/components/BrandWordmark';
import { BrandLogo } from '../src/components/BrandLogo';

const { width } = Dimensions.get('window');

export default function VerifiedScreen() {
    const { t } = useTranslation();
    const { user } = useAuthStore();
    const { setVerificationInProgress } = useNavigationStore();
    const scale = useSharedValue(1);
    const float = useSharedValue(0);

    useEffect(() => {
        scale.value = withRepeat(
            withSequence(
                withTiming(1.05, { duration: 1500 }),
                withTiming(1, { duration: 1500 })
            ),
            -1,
            true
        );
        float.value = withRepeat(
            withSequence(
                withTiming(10, { duration: 2000 }),
                withTiming(0, { duration: 2000 })
            ),
            -1,
            true
        );
    }, []);

    const glowStyle = useAnimatedStyle(() => ({
        transform: [{ scale: scale.value }],
        opacity: withTiming(0.4, { duration: 1500 }),
    }));

    const floatStyle = useAnimatedStyle(() => ({
        transform: [{ translateY: float.value }],
    }));

    const enterPrezzence = () => {
        setVerificationInProgress(false);

        if (!user) {
            router.replace({ pathname: '/auth/sign-in', params: { signup: 'true' } } as any);
            return;
        }

        const settings = useSettingsStore.getState();
        settings.setOnboardingCompleted(false);
        settings.setOnboardingStep('/onboarding/type');
        router.replace('/onboarding/type' as any);
    };

    return (
        <View style={styles.container}>
            <LinearGradient
                colors={['#0A0A0F', '#131321', '#0A0A0F']}
                style={StyleSheet.absoluteFill}
            />

            <SafeAreaView style={styles.safeArea}>
                <View style={styles.content}>
                    <Animated.View entering={FadeIn.duration(700)} style={styles.brandHeader}>
                        <BrandWordmark size={34} textSize={16} />
                    </Animated.View>

                    {/* Atmospheric Glow */}
                    <Animated.View style={[styles.glow, glowStyle]} />

                    <Animated.View
                        entering={ZoomIn.duration(1000).springify()}
                        style={[styles.iconWrapper, floatStyle]}
                    >
                        <View style={styles.iconBackground}>
                            <LinearGradient
                                colors={['rgba(108, 99, 255, 0.2)', 'rgba(0, 214, 143, 0.2)']}
                                style={styles.iconGradient}
                            />
                            <Feather name="shield" size={72} color={colors.success} />
                        </View>
                    </Animated.View>

                    <View style={styles.card}>
                        <Animated.View entering={FadeInDown.delay(400).duration(800)} style={styles.textSection}>
                            <Text style={styles.title}>{t?.auth?.accountVerified || 'Email Confirmed'}</Text>
                            <Text style={styles.subtitle}>
                                {t?.auth?.emailVerifiedDesc || 'Your email is confirmed. Continue to set up your interview practice profile.'}
                            </Text>
                        </Animated.View>

                        <Animated.View entering={FadeInDown.delay(800).duration(800)} style={styles.perksList}>
                            <View style={styles.perk}>
                                <Feather name="check" size={16} color={colors.success} />
                                <Text style={styles.perkText}>Account email confirmed</Text>
                            </View>
                            <View style={styles.perk}>
                                <Feather name="check" size={16} color={colors.success} />
                                <Text style={styles.perkText}>Interview practice profile ready</Text>
                            </View>
                        </Animated.View>

                        <Animated.View entering={FadeInDown.delay(1200).duration(800)} style={styles.buttonSection}>
                            <Pressable
                                onPress={enterPrezzence}
                                style={({ pressed }) => [
                                    styles.buttonContainer,
                                    pressed && { transform: [{ scale: 0.98 }] }
                                ]}
                            >
                                <LinearGradient
                                    colors={[colors.accent, '#8E7DFF']}
                                    start={{ x: 0, y: 0 }}
                                    end={{ x: 1, y: 0 }}
                                    style={styles.buttonGradient}
                                >
                                    <BrandLogo size={24} />
                                    <Text style={styles.buttonText}>{t?.auth?.enterPrezzence || 'Enter Prezzence'}</Text>
                                    <Feather name="arrow-right" size={20} color="#FFFFFF" style={{ marginLeft: 8 }} />
                                </LinearGradient>
                            </Pressable>
                        </Animated.View>
                    </View>
                </View>
            </SafeAreaView>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: colors.background,
    },
    safeArea: {
        flex: 1,
    },
    content: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: spacing.xl,
    },
    brandHeader: {
        position: 'absolute',
        top: spacing.xl,
        alignSelf: 'center',
        zIndex: 12,
    },
    glow: {
        position: 'absolute',
        width: width * 1.2,
        height: width * 1.2,
        borderRadius: width * 0.6,
        backgroundColor: colors.accent,
        opacity: 0.05,
        top: '10%',
    },
    iconWrapper: {
        marginBottom: spacing.xxl,
        zIndex: 10,
    },
    iconBackground: {
        width: 160,
        height: 160,
        borderRadius: 80,
        backgroundColor: colors.card,
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: colors.glassMedium,
        overflow: 'hidden',
        shadowColor: colors.success,
        shadowOffset: { width: 0, height: 0 },
        shadowOpacity: 0.3,
        shadowRadius: 20,
        elevation: 15,
    },
    iconGradient: {
        ...StyleSheet.absoluteFillObject,
    },
    card: {
        width: '100%',
        backgroundColor: colors.glassLight,
        borderRadius: 32,
        padding: spacing.xl,
        borderWidth: 1,
        borderColor: colors.glassMedium,
        alignItems: 'center',
    },
    textSection: {
        alignItems: 'center',
        marginBottom: spacing.xl,
    },
    title: {
        color: colors.text,
        fontSize: typography.h1,
        fontWeight: '900',
        textAlign: 'center',
        marginBottom: spacing.md,
        letterSpacing: -1,
    },
    subtitle: {
        color: colors.textMuted,
        fontSize: 17,
        textAlign: 'center',
        lineHeight: 26,
        opacity: 0.9,
    },
    perksList: {
        width: '100%',
        gap: spacing.md,
        marginBottom: spacing.xl,
    },
    perk: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(255, 255, 255, 0.03)',
        padding: 12,
        borderRadius: 16,
        gap: 12,
    },
    perkText: {
        color: colors.textSoft,
        fontSize: 14,
        fontWeight: '600',
    },
    buttonSection: {
        width: '100%',
    },
    buttonContainer: {
        width: '100%',
        height: 64,
        borderRadius: radii.pill,
        overflow: 'hidden',
    },
    buttonGradient: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
    },
    buttonText: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '800',
        letterSpacing: 0.5,
    },
});
