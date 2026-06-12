import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import * as Linking from 'expo-linking';
import React, { useState, useEffect } from 'react';
import { AppState } from 'react-native';
import Animated, { FadeIn, FadeOut, ZoomIn } from 'react-native-reanimated';
import { useTranslation } from '../../src/hooks/useTranslation';
import {
    ScrollView,
    StyleSheet,
    Text,
    View,
    Pressable,
    TextInput,
    KeyboardAvoidingView,
    Platform
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { supabase } from '../../src/lib/supabase';
import { BrandLogo } from '../../src/components/BrandLogo';
import { useSettingsStore } from '../../src/store/settingsStore';
import { useAuthStore } from '../../src/store/authStore';

const Colors = {
    bgPrimary: '#0A0A0F',
    accent: '#6C63FF',
    textSecondary: '#8A8A9A',
    card: '#1C1C2E',
    success: '#00D68F',
    border: '#2A2A3E',
    danger: '#FF4757',
};

function PasswordRequirement({ label, met }: { label: string, met: boolean }) {
    return (
        <View style={styles.requirementRow}>
            <Feather
                name={met ? "check-circle" : "circle"}
                size={14}
                color={met ? Colors.success : Colors.textSecondary}
                style={styles.requirementIcon}
            />
            <Text style={[styles.requirementText, met && { color: '#FFFFFF', opacity: 1 }]}>
                {label}
            </Text>
        </View>
    );
}

function SuccessView({ onComplete, email, password }: { onComplete: () => void, email: string; password: string }) {
    const { t } = useTranslation();
    const { setSession, setUser, setLoading } = useAuthStore();
    const [resending, setResending] = useState(false);
    const [resendStatus, setResendStatus] = useState<'idle' | 'success' | 'error'>('idle');
    const [checkingVerification, setCheckingVerification] = useState(false);
    const [autoStatus, setAutoStatus] = useState('Waiting for email confirmation');
    const checkInFlightRef = React.useRef(false);

    const continueAfterVerified = React.useCallback((session: any, user: any) => {
        const settings = useSettingsStore.getState();
        settings.setOnboardingCompleted(false);
        settings.setOnboardingStep('/onboarding/type');
        settings.clearSignupCredentials();
        setSession(session);
        setUser(user);
        setLoading(false);
        router.replace('/onboarding/type' as any);
    }, [setLoading, setSession, setUser]);

    const checkVerification = React.useCallback(async (manual = false) => {
        if (checkInFlightRef.current || !email || !password) return;
        checkInFlightRef.current = true;
        if (manual) setCheckingVerification(true);
        try {
            const { data, error } = await supabase.auth.signInWithPassword({
                email,
                password,
            });

            if (error) {
                const message = error.message || '';
                if (/confirm|verified|email/i.test(message)) {
                    setAutoStatus('Still waiting for email confirmation');
                    return;
                }
                if (manual) setAutoStatus(message);
                return;
            }

            if (data.session) {
                setAutoStatus('Email confirmed. Opening Prezzence...');
                continueAfterVerified(data.session, data.user);
            }
        } catch (err: any) {
            if (manual) setAutoStatus(err?.message || 'Unable to check verification right now.');
        } finally {
            checkInFlightRef.current = false;
            if (manual) setCheckingVerification(false);
        }
    }, [continueAfterVerified, email, password]);

    useEffect(() => {
        const timer = setInterval(() => {
            void checkVerification(false);
        }, 4500);
        const subscription = AppState.addEventListener('change', (state) => {
            if (state === 'active') {
                void checkVerification(false);
            }
        });

        return () => {
            clearInterval(timer);
            subscription.remove();
        };
    }, [checkVerification]);

    const handleResend = async () => {
        if (resending) return;
        setResending(true);
        try {
            const { error } = await supabase.auth.resend({
                type: 'signup',
                email: email,
            });
            if (error) throw error;
            setResendStatus('success');
            setTimeout(() => setResendStatus('idle'), 3000);
        } catch (err) {
            console.error('[Resend Error]', err);
            setResendStatus('error');
        } finally {
            setResending(false);
        }
    };

    return (
        <Animated.View
            entering={FadeIn}
            exiting={FadeOut}
            style={[StyleSheet.absoluteFill, styles.successOverlay]}
        >
            <View style={{ alignItems: 'center', paddingHorizontal: 32, width: '100%' }}>
                <Animated.View entering={ZoomIn.delay(200)} style={styles.successIconCircle}>
                    <Feather name="mail" size={60} color={Colors.success} />
                </Animated.View>

                <Animated.View entering={FadeIn.delay(400)}>
                    <Text style={styles.successTitle}>{t.auth.verifyEmail}</Text>
                </Animated.View>

                <Animated.View entering={FadeIn.delay(600)}>
                    <Text style={styles.successSubtitle}>
                        Open the confirmation email, then return here to sign in. If the email opens a browser instead of the app, your account is still confirmed.
                    </Text>
                </Animated.View>

                <Animated.View entering={FadeIn.delay(800)}>
                    <Text style={styles.emailSentTo}>Sent to {email}</Text>
                </Animated.View>

                <Animated.View entering={FadeIn.delay(1200)} style={{ width: '100%', marginTop: 60, paddingHorizontal: 20, alignItems: 'center' }}>
                    <Text style={styles.waitingNotice}>
                        {checkingVerification ? 'Checking verification...' : autoStatus}
                    </Text>

                    <Pressable
                        onPress={handleResend}
                        disabled={resending}
                        style={{ marginTop: 40 }}
                    >
                        <Text style={[
                            styles.resendText,
                            resendStatus === 'success' && { color: Colors.success },
                            resendStatus === 'error' && { color: Colors.danger }
                        ]}>
                            {resending ? 'Sending...' :
                                resendStatus === 'success' ? 'Email resent!' :
                                    resendStatus === 'error' ? 'Error. Try again.' :
                                        "Didn't get the email? Resend"}
                        </Text>
                    </Pressable>

                    <Pressable
                        onPress={() => checkVerification(true)}
                        disabled={checkingVerification}
                        style={{ marginTop: 24, opacity: checkingVerification ? 0.5 : 1 }}
                    >
                        <Text style={styles.smallFooterLink}>
                            {checkingVerification ? 'Checking...' : 'I verified my email - Continue'}
                        </Text>
                    </Pressable>

                    <Pressable
                        onPress={onComplete}
                        style={{ marginTop: 18, opacity: 0.55 }}
                    >
                        <Text style={styles.smallFooterLink}>Go to sign in instead</Text>
                    </Pressable>
                </Animated.View>
            </View>
        </Animated.View>
    );
}

export default function SignUpScreen() {
    const { t } = useTranslation();
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isSuccess, setIsSuccess] = useState(false);

    const handleSignUp = async () => {
        if (!name || !email || !password) {
            setError('Please fill in all fields');
            return;
        }

        if (password.length < 8) {
            setError('Password must be at least 8 characters');
            return;
        }

        setLoading(true);
        setError(null);
        if (__DEV__) {
            console.log('[Auth] Attempting sign up');
        }

        try {
            const { data, error: signUpError } = await supabase.auth.signUp({
                email,
                password,
                options: {
                    emailRedirectTo: Linking.createURL('verify'),
                    data: {
                        full_name: name,
                    }
                }
            });

            if (signUpError) {
                if (__DEV__) {
                    console.error('[Auth Error] Sign up rejected:', signUpError.message);
                }
                throw signUpError;
            }

            if (data.user) {
                // If identities is an empty array, it means the user already exists
                // (Supabase returns a user object but no identities for security, but we can use this to detect duplicates)
                if (data.user.identities && data.user.identities.length === 0) {
                    if (__DEV__) {
                        console.log('[Auth] Detected duplicate email sign up attempt');
                    }
                    setError('An account with this email already exists.');
                    setLoading(false);
                    return;
                }

                if (__DEV__) {
                    console.log('[Auth Success] User created');
                }
                const { setSignupCredentials, setOnboardingCompleted, setOnboardingStep } = useSettingsStore.getState();
                setSignupCredentials(email, password);
                setOnboardingCompleted(false);
                setOnboardingStep('/onboarding/type');
                setIsSuccess(true);
            }
        } catch (err: any) {
            if (__DEV__) {
                console.error('[Auth Exception]', err);
            }
            setError(err.message || 'An error occurred during sign up');
        } finally {
            setLoading(false);
        }
    };

    const handleSuccessComplete = () => {
        router.replace({
            pathname: '/auth/sign-in',
            params: { email, password, signup: 'true' }
        } as any);
    };

    return (
        <SafeAreaView style={styles.container}>
            {isSuccess && <SuccessView onComplete={handleSuccessComplete} email={email} password={password} />}
            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
                style={{ flex: 1 }}
            >
                <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                    {/* Header */}
                    <View style={styles.header}>
                        <Pressable onPress={() => router.back()} style={styles.circleBtn}>
                            <Feather name="chevron-left" size={24} color="#FFFFFF" />
                        </Pressable>
                        <View style={{ flex: 1, alignItems: 'flex-end' }}>
                            <BrandLogo size={32} />
                        </View>
                    </View>

                    {/* Title Section */}
                    <View style={styles.titleSection}>
                        <Text style={styles.headline}>{t.auth.joinPrezzence}</Text>
                        <Text style={styles.subheadline} numberOfLines={1} adjustsFontSizeToFit>
                            {t.auth.startJourney}
                        </Text>
                    </View>

                    {/* Form */}
                    <View style={styles.form}>
                        {error && (
                            <View style={styles.errorBox}>
                                <Feather name="alert-circle" size={16} color="#FF4757" />
                                <Text style={styles.errorText}>{error}</Text>
                            </View>
                        )}

                        <View style={styles.inputGroup}>
                            <Text style={styles.label}>{t.auth.fullName}</Text>
                            <View style={styles.inputWrapper}>
                                <Feather name="user" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder={t.auth.enterName}
                                    placeholderTextColor="rgba(255, 255, 255, 0.2)"
                                    value={name}
                                    onChangeText={setName}
                                    editable={!loading}
                                />
                            </View>
                        </View>

                        <View style={styles.inputGroup}>
                            <Text style={styles.label}>{t.auth.email}</Text>
                            <View style={styles.inputWrapper}>
                                <Feather name="mail" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder="name@example.com"
                                    placeholderTextColor="rgba(255, 255, 255, 0.2)"
                                    value={email}
                                    onChangeText={setEmail}
                                    autoCapitalize="none"
                                    autoCorrect={false}
                                    spellCheck={false}
                                    keyboardType="email-address"
                                    textContentType="emailAddress"
                                    editable={!loading}
                                />
                            </View>
                        </View>

                        <View style={styles.inputGroup}>
                            <Text style={styles.label}>{t.auth.password}</Text>
                            <View style={styles.inputWrapper}>
                                <Feather name="lock" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder={t.auth.createPassword}
                                    placeholderTextColor="rgba(255, 255, 255, 0.2)"
                                    value={password}
                                    onChangeText={setPassword}
                                    secureTextEntry={!showPassword}
                                    editable={!loading}
                                />
                                <Pressable
                                    onPress={() => setShowPassword(!showPassword)}
                                    hitSlop={{ top: 20, bottom: 20, left: 20, right: 20 }}
                                >
                                    <Feather name={showPassword ? "eye-off" : "eye"} size={20} color={Colors.textSecondary} />
                                </Pressable>
                            </View>

                            {/* Requirements */}
                            <View style={styles.requirementsGrid}>
                                <PasswordRequirement label={t.auth.reqLength} met={password.length >= 8} />
                                <PasswordRequirement label={t.auth.reqUpper} met={/[A-Z]/.test(password)} />
                                <PasswordRequirement label={t.auth.reqNumber} met={/\d/.test(password)} />
                                <PasswordRequirement label={t.auth.reqSpecial} met={/[!@#$%^&*]/.test(password)} />
                            </View>
                        </View>

                        <Pressable
                            onPress={handleSignUp}
                            disabled={loading}
                            style={({ pressed }) => [
                                styles.primaryButton,
                                (pressed || loading) && { opacity: 0.9 }
                            ]}
                        >
                            <Text style={styles.buttonText}>{loading ? t.common.loading : t.auth.createAccount}</Text>
                        </Pressable>
                    </View>

                    {/* Footer Link */}
                    <View style={styles.footerLinkRow}>
                        <Text style={styles.footerText}>{t.auth.alreadyHaveAccount} </Text>
                        <Pressable onPress={() => router.push('/auth/sign-in' as any)}>
                            <Text style={styles.footerLinkText}>{t.auth.signIn}</Text>
                        </Pressable>
                    </View>

                    {/* Legal */}
                    <Text style={styles.legalText}>
                        {t.auth.legalNotice}
                        <Text style={styles.legalLink} onPress={() => router.push('/legal/terms' as any)}>{t.auth.termsOfService}</Text>
                        {t.auth.and}
                        <Text style={styles.legalLink} onPress={() => router.push('/legal/privacy' as any)}>{t.auth.privacyPolicy}</Text>
                        {t.auth.legalNoticeEnd}
                    </Text>
                </ScrollView>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: Colors.bgPrimary,
    },
    scrollContent: {
        paddingBottom: 60,
    },
    header: {
        paddingHorizontal: 20,
        height: 60,
        justifyContent: 'center',
    },
    circleBtn: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: 'rgba(28, 28, 46, 0.6)',
        alignItems: 'center',
        justifyContent: 'center',
    },
    titleSection: {
        paddingHorizontal: 24,
        marginTop: 10,
        marginBottom: 20,
    },
    headline: {
        color: '#FFFFFF',
        fontSize: 32,
        fontWeight: '900',
        letterSpacing: -1,
    },
    subheadline: {
        color: Colors.textSecondary,
        fontSize: 16,
        marginTop: 8,
        lineHeight: 24,
    },
    form: {
        paddingHorizontal: 24,
        gap: 16,
        marginBottom: 24,
    },
    inputGroup: {
        gap: 12,
    },
    label: {
        color: '#FFFFFF',
        fontSize: 14,
        fontWeight: '700',
        opacity: 0.8,
    },
    inputWrapper: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(255, 255, 255, 0.03)',
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderRadius: 16,
        paddingHorizontal: 16,
        height: 54,
    },
    inputIcon: {
        marginRight: 12,
    },
    input: {
        flex: 1,
        color: '#FFFFFF',
        fontSize: 16,
        fontWeight: '600',
    },
    requirementsGrid: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 12,
        marginTop: 4,
    },
    requirementRow: {
        flexDirection: 'row',
        alignItems: 'center',
        minWidth: '45%',
    },
    requirementIcon: {
        marginRight: 6,
    },
    requirementText: {
        color: Colors.textSecondary,
        fontSize: 12,
        fontWeight: '600',
        opacity: 0.6,
    },
    primaryButton: {
        backgroundColor: Colors.accent,
        height: 54,
        borderRadius: 27,
        alignItems: 'center',
        justifyContent: 'center',
        marginTop: 4,
        shadowColor: Colors.accent,
        shadowOpacity: 0.3,
        shadowRadius: 15,
        elevation: 8,
    },
    buttonText: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '700',
    },
    footerLinkRow: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 40,
    },
    footerText: {
        color: Colors.textSecondary,
        fontSize: 16,
    },
    footerLinkText: {
        color: Colors.accent,
        fontSize: 14,
        fontWeight: '700',
    },
    errorBox: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(255, 71, 87, 0.1)',
        padding: 12,
        borderRadius: 12,
        borderWidth: 1,
        borderColor: 'rgba(255, 71, 87, 0.2)',
        gap: 8,
        marginBottom: 16,
    },
    errorText: {
        color: Colors.danger,
        fontSize: 13,
        fontWeight: '600',
    },
    legalText: {
        color: Colors.textSecondary,
        fontSize: 12,
        textAlign: 'center',
        paddingHorizontal: 40,
        marginTop: 20,
        lineHeight: 18,
    },
    legalLink: {
        color: '#FFFFFF',
        fontWeight: '600',
    },
    successOverlay: {
        backgroundColor: Colors.bgPrimary,
        zIndex: 100,
        alignItems: 'center',
        justifyContent: 'center',
    },
    successIconCircle: {
        width: 120,
        height: 120,
        borderRadius: 60,
        backgroundColor: 'rgba(0, 214, 143, 0.1)',
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 2,
        borderColor: Colors.success,
        marginBottom: 32,
    },
    successTitle: {
        color: '#FFFFFF',
        fontSize: 32,
        fontWeight: '900',
        marginBottom: 12,
    },
    successSubtitle: {
        color: Colors.textSecondary,
        fontSize: 18,
        fontWeight: '500',
        textAlign: 'center',
    },
    emailSentTo: {
        color: Colors.accent,
        fontSize: 14,
        fontWeight: '600',
        marginTop: 16,
        opacity: 0.8,
    },
    resendText: {
        color: Colors.textSecondary,
        fontSize: 14,
        fontWeight: '600',
        textAlign: 'center',
    },
    waitingNotice: {
        color: '#FFFFFF',
        fontSize: 16,
        fontWeight: '600',
        opacity: 0.8,
        letterSpacing: 0.5,
    },
    smallFooterLink: {
        color: Colors.textSecondary,
        fontSize: 13,
        fontWeight: '600',
    }
});
