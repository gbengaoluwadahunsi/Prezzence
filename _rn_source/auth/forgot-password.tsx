import { Feather } from '@expo/vector-icons';
import * as Linking from 'expo-linking';
import { router } from 'expo-router';
import React, { useState } from 'react';
import {
    ScrollView,
    StyleSheet,
    Text,
    View,
    Pressable,
    TextInput,
    KeyboardAvoidingView,
    Platform,
    ActivityIndicator
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { BrandLogo } from '../../src/components/BrandLogo';
import { supabase } from '../../src/lib/supabase';

const Colors = {
    bgPrimary: '#0A0A0F',
    accent: '#6C63FF',
    textSecondary: '#8A8A9A',
    card: '#1C1C2E',
    success: '#00D68F',
    border: '#2A2A3E',
};

import { useTranslation } from '../../src/hooks/useTranslation';

export default function ForgotPasswordScreen() {
    const { t } = useTranslation();
    const [email, setEmail] = useState('');
    const [isSent, setIsSent] = useState(false);
    const [loading, setLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');

    const handleSendInstructions = async () => {
        const normalizedEmail = email.trim().toLowerCase();
        if (!normalizedEmail) {
            setErrorMessage('Enter the email address on your account.');
            return;
        }

        setLoading(true);
        setErrorMessage('');
        try {
            const { error } = await supabase.auth.resetPasswordForEmail(normalizedEmail, {
                redirectTo: Linking.createURL('/auth/reset-password'),
            });
            if (error) throw error;
            setIsSent(true);
        } catch (error: any) {
            setErrorMessage(error?.message || 'Unable to send reset instructions. Try again.');
        } finally {
            setLoading(false);
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
                        <Pressable onPress={() => router.back()} style={styles.circleBtn}>
                            <Feather name="chevron-left" size={24} color="#FFFFFF" />
                        </Pressable>
                        <View style={{ flex: 1, alignItems: 'flex-end' }}>
                            <BrandLogo size={32} />
                        </View>
                    </View>

                    {/* Title Section */}
                    <View style={styles.titleSection}>
                        <Text style={styles.headline}>{t.auth.forgotPassword}</Text>
                        <Text style={styles.subheadline} numberOfLines={1} adjustsFontSizeToFit>
                            {isSent
                                ? t.auth.resetInstructionsSent
                                : t.auth.forgotPasswordSubtitle
                            }
                        </Text>
                    </View>

                    {/* Recovery Hero */}
                    <View style={styles.heroPlaceholder}>
                        <View style={[styles.iconCircle, { backgroundColor: 'transparent' }]}>
                            <BrandLogo size={100} />
                        </View>
                    </View>

                    {/* Form / Content */}
                    <View style={styles.content}>
                        {!isSent ? (
                            <>
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
                                        />
                                    </View>
                                </View>

                                <Pressable
                                    disabled={loading}
                                    onPress={handleSendInstructions}
                                    style={({ pressed }) => [
                                        styles.primaryButton,
                                        loading && { opacity: 0.7 },
                                        pressed && { opacity: 0.9 }
                                    ]}
                                >
                                    {loading ? (
                                        <ActivityIndicator color="#FFFFFF" />
                                    ) : (
                                        <Text style={styles.buttonText}>{t.auth.sendInstructions}</Text>
                                    )}
                                </Pressable>
                                {errorMessage ? <Text style={styles.errorText}>{errorMessage}</Text> : null}
                            </>
                        ) : (
                            <>
                                <View style={styles.successCard}>
                                    <Text style={styles.successTitle}>{t.auth.checkInboxTitle}</Text>
                                    <Text style={styles.successText}>
                                        {t.auth.checkInboxDesc}
                                    </Text>
                                </View>

                                <Pressable
                                    onPress={() => router.replace('/auth/sign-in' as any)}
                                    style={({ pressed }) => [
                                        styles.secondaryButton,
                                        pressed && { opacity: 0.9 }
                                    ]}
                                >
                                    <Text style={styles.buttonText}>{t.auth.backToSignIn}</Text>
                                </Pressable>
                            </>
                        )}
                    </View>

                    {/* Footer Link */}
                    <View style={styles.footerRow}>
                        <Pressable onPress={() => router.push('/auth/sign-in' as any)}>
                            <Text style={styles.backLink}>{t.auth.backToSignIn}</Text>
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
    heroPlaceholder: {
        height: 180,
        marginHorizontal: 20,
        borderRadius: 32,
        borderWidth: 2,
        borderColor: 'rgba(255,255,255,0.1)',
        borderStyle: 'dashed',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 24,
    },
    iconCircle: {
        width: 100,
        height: 100,
        borderRadius: 50,
        backgroundColor: 'rgba(108, 99, 255, 0.1)',
        alignItems: 'center',
        justifyContent: 'center',
    },
    content: {
        paddingHorizontal: 24,
        gap: 24,
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
    primaryButton: {
        backgroundColor: Colors.accent,
        height: 54,
        borderRadius: 27,
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: Colors.accent,
        shadowOpacity: 0.3,
        shadowRadius: 15,
        elevation: 8,
    },
    secondaryButton: {
        backgroundColor: Colors.card,
        height: 64,
        borderRadius: 32,
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.1)',
    },
    buttonText: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '700',
    },
    successCard: {
        backgroundColor: 'rgba(0, 214, 143, 0.1)',
        borderRadius: 24,
        padding: 24,
        borderWidth: 1,
        borderColor: 'rgba(0, 214, 143, 0.2)',
    },
    successTitle: {
        color: Colors.success,
        fontSize: 20,
        fontWeight: '800',
        marginBottom: 8,
    },
    successText: {
        color: Colors.textSecondary,
        fontSize: 15,
        lineHeight: 22,
    },
    footerRow: {
        alignItems: 'center',
        marginTop: 40,
    },
    backLink: {
        color: Colors.accent,
        fontSize: 16,
        fontWeight: '800',
    },
    errorText: {
        color: '#FF6B7A',
        fontSize: 14,
        lineHeight: 20,
        textAlign: 'center',
        fontWeight: '600',
    },
});
