import { Feather } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { Href, router } from 'expo-router';
import React, { useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, useWindowDimensions, View, Image, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Svg, { Defs, LinearGradient as SvgLinearGradient, Path, Rect, Stop, Text as SvgText } from 'react-native-svg';
import * as Localization from 'expo-localization';
import { useAuthStore } from '../src/store/authStore';
import { useSettingsStore } from '../src/store/settingsStore';
import { useSessionStore } from '../src/store/sessionStore';
import { useNavigationStore } from '../src/store/navigationStore';

const BASE_WIDTH = 430;

import { BrandLogo } from '../src/components/BrandLogo';

function routeAuthenticatedUser(options: { preferHome?: boolean } = {}) {
  const settings = useSettingsStore.getState();
  if (settings.onboardingCompleted) {
    router.replace('/(tabs)/home');
  } else if (settings.onboardingStep) {
    router.replace(settings.onboardingStep as Href);
  } else if (options.preferHome) {
    settings.setOnboardingCompleted(true);
    router.replace('/(tabs)/home');
  } else {
    settings.setOnboardingStep('/onboarding/type');
    router.replace('/onboarding/type');
  }
}

function GradientHeadline({ width, scale }: { width: number; scale: number }) {
  const fontSize = Math.round(76 * scale);
  const lineGap = Math.round(74 * scale);
  const svgHeight = Math.round(155 * scale);

  return (
    <Svg width={width} height={svgHeight}>
      <Defs>
        <SvgLinearGradient id="headlineTop" x1="0%" y1="0%" x2="100%" y2="0%">
          <Stop offset="0%" stopColor="#6A67FF" />
          <Stop offset="45%" stopColor="#5488FF" />
          <Stop offset="100%" stopColor="#36B8E8" />
        </SvgLinearGradient>
        <SvgLinearGradient id="headlineBottom" x1="0%" y1="0%" x2="100%" y2="0%">
          <Stop offset="0%" stopColor="#31B9E7" />
          <Stop offset="100%" stopColor="#13D3A6" />
        </SvgLinearGradient>
      </Defs>
      <SvgText x="0" y={fontSize} fill="url(#headlineTop)" fontSize={fontSize} fontWeight="900">
        Own the
      </SvgText>
      <SvgText x="0" y={fontSize + lineGap} fill="url(#headlineBottom)" fontSize={fontSize} fontWeight="900">
        room.
      </SvgText>
    </Svg>
  );
}

function BackgroundDecor({ width, height, scale }: { width: number; height: number; scale: number }) {
  const lineColor = 'rgba(88, 84, 143, 0.10)';
  const outlineColor = 'rgba(96, 90, 168, 0.14)';
  const secondaryOutline = 'rgba(76, 71, 131, 0.15)';
  const topCardWidth = Math.round(150 * scale);
  const topCardHeight = Math.round(96 * scale);
  const centerFrameWidth = Math.round(170 * scale);
  const centerFrameHeight = Math.round(420 * scale);

  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFill}>
      <View style={[styles.horizonLine, { top: Math.round(106 * scale), backgroundColor: lineColor }]} />
      <View style={[styles.horizonLine, { top: Math.round(226 * scale), backgroundColor: lineColor }]} />
      <View style={[styles.horizonLine, { top: Math.round(442 * scale), backgroundColor: lineColor }]} />

      <View
        style={[
          styles.topCard,
          {
            top: Math.round(164 * scale),
            left: (width - topCardWidth) / 2,
            width: topCardWidth,
            height: topCardHeight,
            borderColor: outlineColor,
            borderRadius: Math.round(16 * scale),
          },
        ]}
      >
        <View style={[styles.topCardLine, { width: Math.round(102 * scale), top: Math.round(21 * scale) }]} />
        <View style={[styles.topCardLine, { width: Math.round(82 * scale), top: Math.round(43 * scale) }]} />
        <View style={[styles.topCardLine, { width: Math.round(94 * scale), top: Math.round(65 * scale) }]} />
      </View>

      <LinearGradient
        colors={['rgba(0,0,0,0)', 'rgba(73, 58, 168, 0.20)', 'rgba(0,0,0,0)']}
        start={{ x: 0.5, y: 0 }}
        end={{ x: 0.5, y: 1 }}
        style={[
          styles.centerGlow,
          {
            width: Math.round(250 * scale),
            height: Math.round(630 * scale),
            left: (width - Math.round(250 * scale)) / 2,
            bottom: Math.round(44 * scale),
          },
        ]}
      />

      <View
        style={[
          styles.centerFrame,
          {
            width: centerFrameWidth,
            height: centerFrameHeight,
            left: (width - centerFrameWidth) / 2,
            bottom: Math.round(74 * scale),
            borderColor: outlineColor,
            borderRadius: Math.round(28 * scale),
          },
        ]}
      />

      <View
        style={[
          styles.sideFrame,
          {
            width: Math.round(118 * scale),
            height: Math.round(505 * scale),
            left: Math.round(-2 * scale),
            bottom: Math.round(20 * scale),
            borderColor: secondaryOutline,
            borderRadius: Math.round(26 * scale),
            transform: [{ rotate: '8deg' }],
          },
        ]}
      />

      <View
        style={[
          styles.sideFrame,
          {
            width: Math.round(124 * scale),
            height: Math.round(520 * scale),
            right: Math.round(-8 * scale),
            bottom: Math.round(24 * scale),
            borderColor: secondaryOutline,
            borderRadius: Math.round(26 * scale),
            transform: [{ rotate: '-8deg' }],
          },
        ]}
      />

      <View
        style={[
          styles.bottomMask,
          {
            width: Math.round(118 * scale),
            height: Math.round(120 * scale),
            left: Math.round(-12 * scale),
            bottom: Math.round(-12 * scale),
          },
        ]}
      />
      <View
        style={[
          styles.bottomMask,
          {
            width: Math.round(118 * scale),
            height: Math.round(120 * scale),
            right: Math.round(-12 * scale),
            bottom: Math.round(-12 * scale),
          },
        ]}
      />
    </View>
  );
}

export default function SplashScreen() {
  const { width, height } = useWindowDimensions();
  const scale = Math.min(width / BASE_WIDTH, 1.12);
  const size = (value: number) => Math.round(value * scale);
  const compactHeight = height < 760;
  const heroTop = compactHeight ? size(76) : Math.min(size(150), Math.max(size(92), Math.round(height * 0.16)));
  const { user, loading: authLoading } = useAuthStore();
  const { onboardingCompleted, onboardingStep } = useSettingsStore();
  const { verificationInProgress } = useNavigationStore();
  const { setLanguage, language: currentLanguage } = useSessionStore();
  const [checking, setChecking] = useState(true);
  const redirectStartedRef = useRef(false);

  useEffect(() => {
    const fallback = setTimeout(() => {
      if (useAuthStore.getState().loading) {
        console.warn('[Splash] Auth bootstrap is slow; showing splash instead of blocking spinner.');
        setChecking(false);
      }
    }, 3500);

    return () => clearTimeout(fallback);
  }, []);

  useEffect(() => {
    // Detect and set default language from device locale
    const locales = Localization.getLocales();
    if (locales && locales.length > 0) {
      const deviceLang = locales[0].languageCode;
      const supported = ['en', 'es', 'fr', 'de', 'it', 'pt', 'zh', 'ja', 'ko', 'ar', 'hi'];

      if (deviceLang && supported.includes(deviceLang) && currentLanguage === 'en' && deviceLang !== 'en') {
        console.log('[Splash] Auto-detected locale:', deviceLang);
        setLanguage(deviceLang);
      }
    }
  }, []);

  useEffect(() => {
    if (!authLoading) {
      if (user) {
        // Don't auto-redirect if a verification deep link is being processed
        if (verificationInProgress) {
          console.log('[Splash] Skipping auto-redirect: verification in progress');
          setChecking(false);
          return;
        }
        if (redirectStartedRef.current) {
          return;
        }
        redirectStartedRef.current = true;

        // Small delay ensures Root Layout is mounted
        const timer = setTimeout(() => {
          if (useNavigationStore.getState().verificationInProgress) {
            redirectStartedRef.current = false;
            return;
          }

          routeAuthenticatedUser({ preferHome: true });
          setChecking(false);
        }, 500);
        return () => clearTimeout(timer);
      } else {
        setChecking(false);
        redirectStartedRef.current = false;
      }
    }
  }, [user, authLoading, onboardingCompleted, onboardingStep, verificationInProgress]);

  return (
    <SafeAreaView style={styles.safeArea} edges={['top', 'bottom']}>
      <View style={styles.screen}>
        <Image
          source={require('../assets/images/interview_bg.png')}
          style={[StyleSheet.absoluteFill, { opacity: 0.65 }]}
          resizeMode="cover"
        />

        <View style={[styles.content, { paddingHorizontal: size(32), paddingTop: compactHeight ? size(32) : size(56) }]}>
          <View style={styles.brandRow}>
            <BrandLogo size={size(40)} />
            <Text
              style={[styles.brandText, { marginLeft: size(14), fontSize: size(22), flexShrink: 1 }]}
              numberOfLines={1}
            >
              Prezzence
            </Text>
          </View>

          <View style={{ marginTop: heroTop }}>
            <Text style={[styles.meta, { fontSize: size(16), letterSpacing: size(5.2) }]}>V 1.0 · SEASON ONE</Text>

            <View style={{ marginTop: size(30) }}>
              <Text style={[styles.title, { fontSize: size(76), lineHeight: size(74) }]}>Practice{"\n"}the room.</Text>
            </View>

            <View style={{ marginTop: size(8) }}>
              <GradientHeadline width={width - size(56)} scale={scale} />
            </View>

            <Text
              style={[
                styles.subtitle,
                {
                  marginTop: size(34),
                  fontSize: size(18),
                  lineHeight: size(30),
                  maxWidth: size(344),
                },
              ]}
            >
              Practice with voice-led AI interviewers that ask, react, score, and coach your answers before the real interview.
            </Text>
          </View>
        </View>

        <View style={[styles.footer, { paddingHorizontal: size(32), paddingBottom: compactHeight ? size(24) : size(56) }]}>
          <LinearGradient
            colors={['rgba(107, 97, 255, 0.00)', 'rgba(107, 97, 255, 0.38)', 'rgba(107, 97, 255, 0.00)']}
            start={{ x: 0, y: 0.5 }}
            end={{ x: 1, y: 0.5 }}
            style={[styles.buttonHalo, { bottom: size(82), height: size(48) }]}
          />

          <Pressable onPress={() => router.push('/auth/sign-up')} style={({ pressed }) => [styles.buttonPressable, pressed && styles.buttonPressed]}>
            <LinearGradient
              colors={['#7062FF', '#564FF8']}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 0 }}
              style={[
                styles.button,
                {
                  height: size(74),
                  borderRadius: size(37),
                  shadowRadius: size(18),
                },
              ]}
            >
              <View style={styles.buttonInner}>
                <Text style={[styles.buttonText, { fontSize: size(18) }]}>Start Practicing</Text>
                <Feather name="chevron-right" size={size(24)} color="#FFFFFF" />
              </View>
            </LinearGradient>
          </Pressable>

          <Pressable onPress={() => router.push('/auth/sign-in')} style={{ marginTop: size(28), alignSelf: 'center' }}>
            <Text style={[styles.secondaryText, { fontSize: size(17) }]}>I already have an account →</Text>
          </Pressable>
          {(checking || authLoading) ? (
            <View style={styles.loadingOverlay}>
              <ActivityIndicator size="small" color="#FFFFFF" />
              <Text style={styles.loadingText}>Opening Prezzence...</Text>
            </View>
          ) : null}
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#0A0911',
  },
  screen: {
    flex: 1,
    backgroundColor: '#0A0911',
    justifyContent: 'space-between',
  },
  content: {
    flex: 1,
    position: 'relative',
    zIndex: 1,
  },
  brandRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  brandText: {
    color: '#FFFFFF',
    fontWeight: '800',
    letterSpacing: -0.4,
  },
  meta: {
    color: '#675EFB',
    fontWeight: '500',
  },
  title: {
    color: '#F0ECFF',
    fontWeight: '900',
    letterSpacing: -3.2,
  },
  subtitle: {
    color: '#8E8AA7',
    fontWeight: '400',
  },
  footer: {
    position: 'relative',
    zIndex: 2,
  },
  buttonHalo: {
    position: 'absolute',
    left: 10,
    right: 10,
    borderRadius: 999,
  },
  buttonPressable: {
    width: '100%',
  },
  buttonPressed: {
    opacity: 0.94,
  },
  button: {
    borderWidth: 1,
    borderColor: 'rgba(175, 165, 255, 0.55)',
    justifyContent: 'center',
    shadowColor: '#685FFF',
    shadowOpacity: 0.38,
    elevation: 10,
  },
  buttonInner: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  buttonText: {
    color: '#FFFFFF',
    fontWeight: '800',
  },
  secondaryText: {
    color: '#858198',
    fontWeight: '500',
  },
  loadingOverlay: {
    marginTop: 18,
    minHeight: 34,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
  },
  loadingText: {
    color: '#C8C4DD',
    fontSize: 13,
    fontWeight: '800',
  },
  horizonLine: {
    position: 'absolute',
    left: 0,
    right: 0,
    height: 1,
  },
  topCard: {
    position: 'absolute',
    borderWidth: 1,
  },
  topCardLine: {
    position: 'absolute',
    left: 18,
    height: 1,
    backgroundColor: 'rgba(110, 104, 194, 0.12)',
  },
  centerGlow: {
    position: 'absolute',
    borderRadius: 999,
  },
  centerFrame: {
    position: 'absolute',
    borderWidth: 1,
  },
  sideFrame: {
    position: 'absolute',
    borderWidth: 1,
  },
  bottomMask: {
    position: 'absolute',
    backgroundColor: '#0A0911',
  },
});
