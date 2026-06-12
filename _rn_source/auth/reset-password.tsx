import { Feather } from '@expo/vector-icons';
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

export default function ResetPasswordScreen() {
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');

    const handleResetPassword = async () => {
        if (password.length < 8) {
            setErrorMessage('Password must be at least 8 characters.');
            return;
        }
        if (password !== confirmPassword) {
            setErrorMessage('Passwords do not match.');
            return;
        }

        setLoading(true);
        setErrorMessage('');
        try {
            const { error } = await supabase.auth.updateUser({ password });
            if (error) throw error;
            router.replace('/auth/sign-in' as any);
        } catch (error: any) {
            setErrorMessage(error?.message || 'Unable to reset password. Open the latest reset email and try again.');
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
                        <Text style={styles.headline}>Reset password.</Text>
                        <Text style={styles.subheadline} numberOfLines={1} adjustsFontSizeToFit>
                            Choose a strong new password for your account.
                        </Text>
                    </View>

                    {/* Form */}
                    <View style={styles.form}>
                        <View style={styles.inputGroup}>
                            <Text style={styles.label}>New Password</Text>
                            <View style={styles.inputWrapper}>
                                <Feather name="lock" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder="Enter new password"
                                    placeholderTextColor="rgba(255, 255, 255, 0.2)"
                                    value={password}
                                    onChangeText={setPassword}
                                    secureTextEntry={!showPassword}
                                />
                                <Pressable
                                    onPress={() => setShowPassword(!showPassword)}
                                    hitSlop={{ top: 20, bottom: 20, left: 20, right: 20 }}
                                >
                                    <Feather name={showPassword ? "eye-off" : "eye"} size={20} color={Colors.textSecondary} />
                                </Pressable>
                            </View>
                        </View>

                        <View style={styles.inputGroup}>
                            <Text style={styles.label}>Confirm New Password</Text>
                            <View style={styles.inputWrapper}>
                                <Feather name="lock" size={20} color={Colors.textSecondary} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder="Repeat your password"
                                    placeholderTextColor="rgba(255, 255, 255, 0.2)"
                                    value={confirmPassword}
                                    onChangeText={setConfirmPassword}
                                    secureTextEntry={!showPassword}
                                />
                            </View>
                        </View>

                        <Pressable
                            disabled={loading}
                            onPress={handleResetPassword}
                            style={({ pressed }) => [
                                styles.primaryButton,
                                loading && { opacity: 0.7 },
                                pressed && { opacity: 0.9 }
                            ]}
                        >
                            {loading ? (
                                <ActivityIndicator color="#FFFFFF" />
                            ) : (
                                <Text style={styles.buttonText}>Reset Password</Text>
                            )}
                        </Pressable>
                        {errorMessage ? <Text style={styles.errorText}>{errorMessage}</Text> : null}
                    </View>

                    {/* Secondary Action */}
                    <View style={styles.footerRow}>
                        <Pressable onPress={() => router.replace('/auth/sign-in' as any)}>
                            <Text style={styles.cancelLink}>Cancel and return to Sign In</Text>
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
    footerRow: {
        alignItems: 'center',
        marginTop: 40,
    },
    cancelLink: {
        color: Colors.textSecondary,
        fontSize: 15,
        fontWeight: '600',
        opacity: 0.8,
    },
    errorText: {
        color: '#FF6B7A',
        fontSize: 14,
        lineHeight: 20,
        textAlign: 'center',
        fontWeight: '600',
    },
});
