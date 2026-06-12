import React, { useEffect } from 'react';
import { Feather } from '@expo/vector-icons';
import { Tabs, router } from 'expo-router';
import { Platform } from 'react-native';
import { colors } from '../../src/theme';
import { useAuthStore } from '../../src/store/authStore';
import Animated, { useAnimatedStyle, withSpring } from 'react-native-reanimated';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

function TabIcon({ name, color, size, focused }: { name: React.ComponentProps<typeof Feather>['name'], color: string, size: number, focused: boolean }) {
  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: withSpring(focused ? 1.2 : 1) }],
    opacity: withSpring(focused ? 1 : 0.7),
  }));

  return (
    <Animated.View style={animatedStyle}>
      <Feather name={name} color={color} size={size} />
    </Animated.View>
  );
}

export default function TabsLayout() {
  const { user, loading } = useAuthStore();
  const insets = useSafeAreaInsets();
  const bottomInset = Math.max(insets.bottom, Platform.OS === 'android' ? 36 : 14);

  useEffect(() => {
    if (!loading && !user) {
      router.replace('/auth/sign-in');
    }
  }, [user, loading]);

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: 'rgba(255, 255, 255, 0.4)',
        tabBarHideOnKeyboard: true,
        tabBarLabelStyle: {
          fontSize: 10,
          lineHeight: 12,
          fontWeight: '700',
          marginTop: -1,
          marginBottom: 0,
        },
        tabBarStyle: {
          backgroundColor: '#0D0D15',
          borderTopColor: 'rgba(255, 255, 255, 0.05)',
          height: 72 + bottomInset,
          paddingTop: 10,
          paddingBottom: bottomInset + 2,
          borderTopWidth: 1,
          elevation: 0,
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
        },
      }}
    >
      <Tabs.Screen
        name="home"
        options={{
          title: 'Home',
          tabBarIcon: ({ color, focused }) => (
            <TabIcon name="home" color={color} size={24} focused={focused} />
          )
        }}
      />
      <Tabs.Screen
        name="practice"
        options={{
          title: 'Practice',
          tabBarIcon: ({ color, focused }) => (
            <TabIcon name="star" color={color} size={24} focused={focused} />
          )
        }}
      />
      <Tabs.Screen
        name="questions"
        options={{
          href: null,
          title: 'Questions',
          tabBarIcon: ({ color, focused }) => (
            <TabIcon name="help-circle" color={color} size={24} focused={focused} />
          )
        }}
      />
      <Tabs.Screen
        name="progress"
        options={{
          title: 'Progress',
          tabBarIcon: ({ color, focused }) => (
            <TabIcon name="bar-chart-2" color={color} size={24} focused={focused} />
          )
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Profile',
          tabBarIcon: ({ color, focused }) => (
            <TabIcon name="user" color={color} size={24} focused={focused} />
          )
        }}
      />

      {/* Hide redundant/internal routes */}
      <Tabs.Screen name="index" options={{ href: null }} />
    </Tabs>
  );
}
