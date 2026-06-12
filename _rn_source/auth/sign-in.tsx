import { Feather, FontAwesome, AntDesign } from '@expo/vector-icons';
import { useLocalSearchParams, router } from 'expo-router';
import * as Linking from 'expo-linking';
import React, { useState, useEffect } from 'react';
import type { Provider } from '@supabase/supabase-js';
import { useTranslation } from '../../src/hooks/useTranslation';
import { useAuthStore } from '../../src/store/authStore';
import { useSettingsStore } from '../../src/store/settingsStore';
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
import { LinearGradient } from 'expo-linear-gradient';
import { supabase } from '../../src/lib/supabase';
import { BrandLogo } from '../../src/components/BrandLogo';

const Colors = {
    bgPrimary: '#0A0A0F',
    accent: '#6C63FF',
    textSecondary: '#8A8A9A',
    card: '#1C1C2E',
    success: '#00D68F',
    border: '#2A2A3E',
    danger: '#FF4757',
};

function routeAfterSuccessfulSignIn(fromSignupFlow: boolean) {
    const settings = useSettingsStore.getState();

    if (settings.onboardingCompleted) {
        router.replace('/(tabs)/home' as any);
        return;
    }

    if (settings.onboardingStep) {
        router.replace(settings.onboardingStep as any);
        return;
    }

    if (fromSignupFlow) {
        settings.setOnboardingStep('/onboarding/type');
        router.replace('/onboarding/type' as any);
        return;
    }

    settings.setOnboardingCompleted(true);
    router.replace('/(tabs)/home' as any);
}

export default function SignInScreen() {
    const { setUser, setSession, setLoading } = useAuthStore();
    const { setSignupCredentials } = useSettingsStore();
    const { t } = useTranslation();
    const params = useLocalSearchParams<{ email?: string; password?: string; signup?: string; verified?: string; error?: string }>();
    const [email, setEmail] = useState(params.email || '');
    const [password, setPassword] = useState(params.password || '');
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoadingState] = useState(false);
    const [socialLoading, setSocialLoading] = useState<Provider | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [signupSuccess, setSignupSuccess] = useState(params.signup === 'true');
    const visibleSocialProviders: Extract<Provider, 'google' | 'apple'>[] =
        Platform.OS === 'ios' ? ['apple'] : Platform.OS === 'android' ? ['google'] : ['google', 'apple'];

    useEffect(() => {
        if (params.email) setEmail(params.email);
        if (params.password) setPassword(params.password);
        if (params.signup === 'true') setSignupSuccess(true);
        if (params.verified === 'true') setSignupSuccess(true);
        if (params.error) setError(String(params.error));

        // Also check persisted credentials for deep links
        const { signupEmail, signupPassword } = useSettingsStore.getState();
        if (signupEmail && !email) setEmail(signupEmail);
        if (signupPassword && !password) setPassword(signupPassword);

        // Clear them once used to avoid confusing future logins
        if (signupEmail || signupPassword) {
            setSignupCredentials(null, null);
        }
    }, [params]);

    const handleSignIn = async () => {
        if (!email || !password) {
            setError('Please fill in all fields');
            return;
        }

        setLoadingState(true);
        setError(null);

        try {
            const { data, error: signInError } = await supabase.auth.signInWithPassword({
                email,
                password,
            });

            if (signInError) throw signInError;

            if (data.session) {
                setSession(data.session);
                setUser(data.user);
                routeAfterSuccessfulSignIn(signupSuccess || params.signup === 'true');
            }
        } catch (err: any) {
            setError(err.message || 'An error occurred during sign in');
        } finally {
            setLoading(false);
            setLoadingState(false);
        }
    };

    const handleOAuthSignIn = async (provider: Extract<Provider, 'google' | 'apple'>) => {
        if (socialLoading || loading) return;

        setSocialLoading(provider);
        setError(null);

        try {
            const redirectTo = Platform.OS === 'web' && typeof window !== 'undefined'
                ? window.location.origin
                : Linking.createURL('auth/callback');

            const { data, error: oauthError } = await supabase.auth.signInWithOAuth({
                provider,
                options: {
                    redirectTo,
                    skipBrowserRedirect: true,
                },
            });

            if (oauthError) throw oauthError;
            if (!data.url) throw new Error(`Could not start ${provider} sign in`);

            await Linking.openURL(data.url);
        } catch (err: any) {
            console.error(`[OAuth] ${provider} sign in failed:`, err);
            setError(err.message || `Could not continue with ${provider}`);
        } finally {
            setSocialLoading(null);
        }
    };

    return (
        <SafeAreaView style={styles.container}>
            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
                style={{ flex: 1 }}
            >
                <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                    {/* Header */}
                    <View style={styles.header}>
                        <Pressable onPress={() => router.replace('/')} style={styles.circleBtn}>
                            <Feather name="x" size={24} color="#FFFFFF" />
                        </Pressable>
                        <View style={{ flex: 1, alignItems: 'flex-end' }}>
                            <BrandLogo size={32} />
                        </View>
                    </View>

                    {/* Title Section */}
                    <View style={styles.titleSection}>
                        <Text style={styles.headline}>{t.auth.welcomeBack}</Text>
                        <Text style={styles.subheadline} numberOfLines={1} adjustsFontSizeToFit>
                            {t.onboarding.languageSubheadline || 'Sign in to continue your journey.'}
                        </Text>
                    </View>

                    {/* Form */}
                    <View style={styles.form}>
                        {signupSuccess && (
                            <View style={[styles.errorBox, { backgroundColor: 'rgba(0, 214, 143, 0.1)', borderColor: 'rgba(0, 214, 143, 0.2)' }]}>
                                <Feather name="check-circle" size={16} color={Colors.success} />
                                <Text style={[styles.errorText, { color: Colors.success, flex: 1 }]}>
                                    {params.verified === 'true'
                                        ? 'Email confirmed. Sign in to continue.'
                                        : 'Verification email sent. Confirm your email, then sign in.'}
                                </Text>
                            </View>
                        )}
                        {error && (
                            <View style={styles.errorBox}>
                                <Feather name="alert-circle" size={16} color="#FF4757" />
                                <Text style={styles.errorText}>{error}</Text>
                            </View>
                        )}

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
                            <View style={styles.labelRow}>
                                <Text style={styles.label}>{t.auth.password}</Text>
                                <Pressable onPress={() => router.push('/auth/forgot-password' as any)}>
                                    <Text style={styles.forgotText}>{t.auth.forgotPassword}</Text>
                                </Pressable>
                            </View>
                            <View style={styles.inputWrapper}>
                                <Feather name="lock" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder={t.auth.enterPassword}
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
                        </View>

                        <Pressable
                            onPress={handleSignIn}
                            disabled={loading}
                            style={({ pressed }) => [
                                styles.primaryButton,
                                (pressed || loading) && { opacity: 0.9 }
                            ]}
                        >
                            <Text style={styles.buttonText}>{loading ? t.common.loading : t.auth.signIn}</Text>
                        </Pressable>
                    </View>

                    {/* Divider */}
                    <View style={styles.dividerRow}>
                        <View style={styles.divider} />
                        <Text
                            style={styles.dividerText}
                            numberOfLines={1}
                            adjustsFontSizeToFit
                        >
                            {t.auth.orContinueWith}
                        </Text>
                        <View style={styles.divider} />
                    </View>

                    {/* Social Buttons */}
                    <View style={[styles.socialRow, visibleSocialProviders.length === 1 && styles.singleSocialRow]}>
                        {visibleSocialProviders.map((provider) => (
                            <Pressable
                                key={provider}
                                onPress={() => handleOAuthSignIn(provider)}
                                disabled={loading || !!socialLoading}
                                style={({ pressed }) => [
                                    styles.socialBtn,
                                    visibleSocialProviders.length === 1 && styles.singleSocialBtn,
                                    (pressed || socialLoading === provider) && styles.socialBtnPressed,
                                    (loading || !!socialLoading) && styles.socialBtnDisabled,
                                ]}
                            >
                                <FontAwesome name={provider} size={20} color="#FFFFFF" />
                                <Text style={styles.socialBtnText}>
                                    {socialLoading === provider ? 'Opening...' : provider === 'apple' ? 'Apple' : 'Google'}
                                </Text>
                            </Pressable>
                        ))}
                    </View>

                    {/* Footer Link */}
                    <View style={styles.footerLinkRow}>
                        <Text style={styles.footerText}>{t.auth.dontHaveAccount} </Text>
                        <Pressable onPress={() => router.push('/auth/sign-up' as any)}>
                            <Text style={styles.footerLinkText}>{t.auth.signUp}</Text>
                        </Pressable>
                    </View>
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
        paddingBottom: 40,
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
    },
    inputGroup: {
        gap: 12,
    },
    labelRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    label: {
        color: '#FFFFFF',
        fontSize: 14,
        fontWeight: '700',
        opacity: 0.8,
    },
    forgotText: {
        color: Colors.accent,
        fontSize: 14,
        fontWeight: '700',
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
    primaryButton: {
        backgroundColor: Colors.accent,
        height: 54,
        borderRadius: 27,
        alignItems: 'center',
        justifyContent: 'center',
        marginTop: 8,
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
    dividerRow: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 24,
        marginTop: 24,
        marginBottom: 20,
    },
    divider: {
        flex: 1,
        height: 1,
        backgroundColor: 'rgba(255, 255, 255, 0.1)',
    },
    dividerText: {
        color: Colors.textSecondary,
        fontSize: 14,
        paddingHorizontal: 20,
        flexShrink: 1,
    },
    socialRow: {
        flexDirection: 'row',
        paddingHorizontal: 24,
        gap: 16,
        marginBottom: 40,
    },
    singleSocialRow: {
        justifyContent: 'center',
    },
    socialBtn: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.1)',
        height: 54,
        borderRadius: 27,
        gap: 12,
    },
    singleSocialBtn: {
        flex: 0,
        width: '100%',
    },
    socialBtnPressed: {
        opacity: 0.86,
    },
    socialBtnDisabled: {
        opacity: 0.6,
    },
    socialBtnText: {
        color: '#FFFFFF',
        fontSize: 16,
        fontWeight: '700',
    },
    footerLinkRow: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
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
    },
    errorText: {
        color: Colors.danger,
        fontSize: 13,
        fontWeight: '600',
    },
});
