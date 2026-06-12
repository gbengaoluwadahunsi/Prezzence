import { Stack, router } from 'expo-router';
import React, { useEffect } from 'react';
import { useAuthStore } from '../../src/store/authStore';

export default function OnboardingLayout() {
    const { user, loading } = useAuthStore();

    useEffect(() => {
        if (!loading && !user) {
            router.replace('/auth/sign-in');
        }
    }, [user, loading]);
    return (
        <Stack
            screenOptions={{
                headerShown: false,
                contentStyle: { backgroundColor: '#0A0A0F' },
                animation: 'slide_from_right',
            }}
        />
    );
}
