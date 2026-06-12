import { Feather } from '@expo/vector-icons';
import { Link, Stack } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, View, Pressable } from 'react-native';
import { colors, spacing, typography, radii } from '../src/theme';

export default function NotFoundScreen() {
    return (
        <>
            <Stack.Screen options={{ title: 'Oops!', headerShown: false }} />
            <View style={styles.container}>
                <View style={styles.voidCircle}>
                    <Feather name="anchor" size={64} color={colors.accent} />
                    <View style={styles.orbit} />
                </View>

                <Text style={styles.title}>Page not found</Text>
                <Text style={styles.subtitle}>
                    This page does not exist or the link is no longer available.
                </Text>

                <Link href="/" asChild>
                    <Pressable style={styles.button}>
                        <Text style={styles.buttonText}>Return home</Text>
                    </Pressable>
                </Link>
            </View>
        </>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        padding: 32,
        backgroundColor: colors.background,
    },
    voidCircle: {
        width: 160,
        height: 160,
        borderRadius: 80,
        backgroundColor: 'rgba(108, 99, 255, 0.05)',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 48,
    },
    orbit: {
        position: 'absolute',
        width: 180,
        height: 180,
        borderRadius: 90,
        borderWidth: 1,
        borderColor: 'rgba(108, 99, 255, 0.1)',
        borderStyle: 'dashed',
    },
    title: {
        fontSize: 32,
        fontWeight: '900',
        color: '#FFFFFF',
        marginBottom: 16,
        letterSpacing: -1,
    },
    subtitle: {
        fontSize: 16,
        color: colors.textMuted,
        textAlign: 'center',
        lineHeight: 24,
        marginBottom: 48,
        opacity: 0.8,
    },
    button: {
        backgroundColor: colors.card,
        paddingHorizontal: 32,
        paddingVertical: 18,
        borderRadius: 32,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.1)',
    },
    buttonText: {
        fontSize: 16,
        fontWeight: '800',
        color: '#FFFFFF',
    },
});
