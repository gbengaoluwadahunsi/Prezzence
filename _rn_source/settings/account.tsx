import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React, { useState, useEffect } from 'react';
import {
    ScrollView,
    StyleSheet,
    Text,
    View,
    Pressable,
    TextInput,
    ActivityIndicator
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors, spacing, typography, radii } from '../../src/theme';
import { useAuthStore } from '../../src/store/authStore';
import { supabase } from '../../src/lib/supabase';

export default function AccountSettingsScreen() {
    const { user, setUser } = useAuthStore();
    const [name, setName] = useState(user?.user_metadata?.full_name || '');
    const [email] = useState(user?.email || '');
    const [focus, setFocus] = useState(user?.user_metadata?.focus || '');
    const [loading, setLoading] = useState(false);

    const handleSave = async () => {
        setLoading(true);
        try {
            const { data, error } = await supabase.auth.updateUser({
                data: {
                    full_name: name.trim(),
                    focus: focus.trim(),
                },
            });
            if (error) throw error;
            if (data.user) setUser(data.user);
            router.back();
        } catch (err) {
            console.error('[Account] Save failed:', err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Pressable onPress={() => router.back()} style={styles.backBtn}>
                    <Feather name="chevron-left" size={24} color={colors.accent} />
                </Pressable>
                <Text style={styles.headerTitle}>Account details</Text>
                <View style={{ width: 44 }} />
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                <View style={styles.avatarSection}>
                    <View style={styles.avatarHalo}>
                        <View style={styles.avatarCircle}>
                            <Feather name="user" size={48} color={colors.accent} />
                        </View>
                    </View>
                    <Text style={styles.userName}>{name || 'Prezzence user'}</Text>
                    <Text style={styles.userRole}>{email || 'Signed in account'}</Text>
                </View>

                <View style={styles.formSection}>
                    <Text style={styles.sectionLabel}>PROFILE DETAILS</Text>

                    <View style={styles.inputGroup}>
                        <Text style={styles.fieldLabel}>Display Name</Text>
                        <View style={styles.inputBox}>
                            <TextInput
                                style={styles.input}
                                placeholder="Enter name"
                                placeholderTextColor={colors.textMuted}
                                value={name}
                                onChangeText={setName}
                            />
                        </View>
                    </View>

                    <View style={styles.inputGroup}>
                        <Text style={styles.fieldLabel}>Email address</Text>
                        <View style={styles.inputBox}>
                            <TextInput
                                style={styles.input}
                                placeholder="Enter email"
                                placeholderTextColor={colors.textMuted}
                                value={email}
                                keyboardType="email-address"
                                editable={false} // Identity locked for security
                            />
                        </View>
                    </View>

                    <View style={styles.inputGroup}>
                        <Text style={styles.fieldLabel}>Main interview focus</Text>
                        <View style={styles.inputBox}>
                            <TextInput
                                style={styles.input}
                                placeholder="Enter focus"
                                placeholderTextColor={colors.textMuted}
                                value={focus}
                                onChangeText={setFocus}
                            />
                        </View>
                    </View>
                </View>

                <View style={styles.dangerZone}>
                    <Text style={[styles.sectionLabel, { color: colors.danger }]}>ACCOUNT ACTIONS</Text>
                    <Pressable style={styles.deleteBtn} onPress={() => router.push('/profile/delete-account' as any)}>
                        <Feather name="trash-2" size={18} color={colors.danger} />
                        <Text style={styles.deleteText}>Delete account</Text>
                    </Pressable>
                </View>
            </ScrollView>

            <View style={styles.footer}>
                <Pressable
                    onPress={handleSave}
                    disabled={loading}
                    style={({ pressed }) => [
                        styles.saveButton,
                        (pressed || loading) && { opacity: 0.9 }
                    ]}
                >
                    {loading ? (
                        <ActivityIndicator color={colors.text} />
                    ) : (
                        <>
                            <Text style={styles.saveText}>Save changes</Text>
                            <Feather name="shield" size={18} color={colors.text} style={{ marginLeft: 10 }} />
                        </>
                    )}
                </Pressable>
            </View>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: colors.background,
    },
    scrollContent: {
        paddingHorizontal: 24,
        paddingBottom: 120,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        height: 80,
    },
    backBtn: {
        width: 44,
        height: 44,
        backgroundColor: colors.card,
        borderRadius: 22,
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: colors.glassLight,
    },
    headerTitle: {
        color: colors.text,
        fontSize: 16,
        fontWeight: '800',
    },
    avatarSection: {
        alignItems: 'center',
        marginTop: 20,
        marginBottom: 40,
    },
    avatarHalo: {
        width: 120,
        height: 120,
        borderRadius: 60,
        borderWidth: 1,
        borderColor: colors.accent,
        alignItems: 'center',
        justifyContent: 'center',
        padding: 4,
        marginBottom: 20,
    },
    avatarCircle: {
        width: '100%',
        height: '100%',
        borderRadius: 55,
        backgroundColor: colors.card,
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: colors.glassMedium,
    },
    editBtn: {
        position: 'absolute',
        bottom: 0,
        right: 0,
        width: 36,
        height: 36,
        borderRadius: 18,
        backgroundColor: colors.accent,
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 3,
        borderColor: colors.background,
    },
    userName: {
        color: colors.text,
        fontSize: 24,
        fontWeight: '900',
        letterSpacing: -0.5,
    },
    userRole: {
        color: colors.accent,
        fontSize: 14,
        fontWeight: '800',
        marginTop: 4,
        letterSpacing: 1,
        textTransform: 'uppercase',
    },
    formSection: {
        gap: 24,
        marginBottom: 48,
    },
    sectionLabel: {
        color: colors.accent,
        fontSize: 10,
        fontWeight: '900',
        letterSpacing: 2,
    },
    inputGroup: {
        gap: 10,
    },
    fieldLabel: {
        color: colors.textMuted,
        fontSize: 13,
        fontWeight: '700',
        marginLeft: 4,
    },
    inputBox: {
        backgroundColor: colors.card,
        borderRadius: 24,
        borderWidth: 1,
        borderColor: colors.glassLight,
        height: 60,
        paddingHorizontal: 20,
        justifyContent: 'center',
    },
    input: {
        color: colors.text,
        fontSize: 16,
        fontWeight: '700',
    },
    dangerZone: {
        marginBottom: 40,
    },
    deleteBtn: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 10,
        height: 60,
        backgroundColor: 'rgba(255, 90, 122, 0.05)',
        borderRadius: 24,
        borderWidth: 1,
        borderColor: 'rgba(255, 90, 122, 0.1)',
        marginTop: 16,
    },
    deleteText: {
        color: colors.danger,
        fontSize: 16,
        fontWeight: '800',
    },
    footer: {
        position: 'absolute',
        bottom: 0,
        left: 0,
        right: 0,
        padding: 24,
        paddingBottom: 40,
        backgroundColor: 'rgba(0,0,0,0.8)',
    },
    saveButton: {
        backgroundColor: colors.accent,
        height: 64,
        borderRadius: 32,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: colors.accent,
        shadowOpacity: 0.4,
        shadowRadius: 15,
        elevation: 10,
    },
    saveText: {
        color: colors.text,
        fontSize: 18,
        fontWeight: '800',
    },
});
