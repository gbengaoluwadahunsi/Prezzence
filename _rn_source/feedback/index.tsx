import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React, { useState } from 'react';
import {
    ScrollView,
    StyleSheet,
    Text,
    View,
    Pressable,
    TextInput
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Svg, { Circle } from 'react-native-svg';
import { submitFeedback } from '../../src/api/feedback';
import { Button } from '../../src/components/Button';
import { colors } from '../../src/theme';

const Colors = {
    bgPrimary: '#0A0A0F',
    accent: '#6C63FF',
    textSecondary: '#8A8A9A',
    card: '#1C1C2E',
    success: '#00D68F',
    border: '#2A2A3E',
};

export default function RateAppScreen() {
    const [rating, setRating] = useState(5);
    const [comment, setComment] = useState('');
    const [loading, setLoading] = useState(false);
    const [submitted, setSubmitted] = useState(false);

    const handleSubmit = async () => {
        try {
            setLoading(true);
            await submitFeedback({ rating, comment });
            setSubmitted(true);
            setTimeout(() => router.back(), 2000);
        } catch (err) {
            console.error('[Feedback] Submit failed:', err);
        } finally {
            setLoading(false);
        }
    };

    if (submitted) {
        return (
            <SafeAreaView style={styles.container}>
                <View style={styles.centerBox}>
                    <Feather name="check-circle" size={80} color={Colors.success} />
                    <Text style={styles.headline}>Thank You!</Text>
                    <Text style={styles.subheadline}>Your feedback helps us improve the app.</Text>
                </View>
            </SafeAreaView>
        );
    }

    return (
        <SafeAreaView style={styles.container}>
            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                {/* Header */}
                <View style={styles.header}>
                    <Pressable onPress={() => router.back()} style={styles.circleBtn}>
                        <Feather name="chevron-left" size={24} color="#FFFFFF" />
                    </Pressable>
                    <Text style={styles.headerTitle}>Feedback</Text>
                    <View style={{ width: 44 }} />
                </View>

                {/* Progress */}
                <View style={styles.progressBar}>
                    {[...Array(5)].map((_, i) => (
                        <View
                            key={i}
                            style={[styles.progressSegment, i < rating && styles.progressSegmentActive]}
                        />
                    ))}
                </View>

                {/* Title */}
                <View style={styles.titleSection}>
                    <Text style={styles.headline}>How was it?</Text>
                    <Text style={styles.subheadline}>Rate your interview practice experience.</Text>
                </View>

                {/* Rating Picker */}
                <View style={styles.ratingSection}>
                    <View style={styles.svgWrapper}>
                        <Svg height="240" width="240" viewBox="0 0 100 100">
                            <Circle
                                cx="50"
                                cy="50"
                                r="45"
                                stroke="#1C1C2E"
                                strokeWidth="8"
                                fill="none"
                            />
                            <Circle
                                cx="50"
                                cy="50"
                                r="45"
                                stroke={Colors.success}
                                strokeWidth="8"
                                strokeDasharray="282.7"
                                strokeDashoffset={282.7 * (1 - rating / 5)}
                                strokeLinecap="round"
                                fill="none"
                                transform="rotate(-90 50 50)"
                            />
                        </Svg>
                        <View style={styles.ratingNumberBox}>
                            <Text style={styles.ratingNumber}>{rating}</Text>
                        </View>
                    </View>

                    <View style={styles.starsRow}>
                        {[1, 2, 3, 4, 5].map((s) => (
                            <Pressable key={s} onPress={() => setRating(s)}>
                                <Feather
                                    name="star"
                                    size={32}
                                    color={s <= rating ? Colors.success : '#1C1C2E'}
                                    fill={s <= rating ? Colors.success : 'transparent'}
                                />
                            </Pressable>
                        ))}
                    </View>
                </View>

                {/* Comment Section */}
                <View style={styles.inputSection}>
                    <Text style={styles.inputLabel}>COMMENT (OPTIONAL)</Text>
                    <TextInput
                        style={styles.textInputBox}
                        placeholder="What should we improve?"
                        placeholderTextColor="rgba(255,255,255,0.2)"
                        multiline
                        value={comment}
                        onChangeText={setComment}
                    />
                </View>
            </ScrollView>

            <View style={styles.footer}>
                <Button loading={loading} onPress={handleSubmit}>Submit feedback</Button>
            </View>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: Colors.bgPrimary,
    },
    centerBox: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        padding: 40,
        gap: 20,
    },
    scrollContent: {
        paddingBottom: 100,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        height: 60,
        marginTop: 10,
    },
    circleBtn: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        alignItems: 'center',
        justifyContent: 'center',
    },
    headerTitle: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '800',
    },
    progressBar: {
        flexDirection: 'row',
        paddingHorizontal: 24,
        height: 4,
        marginTop: 8,
        gap: 8,
    },
    progressSegment: {
        flex: 1,
        backgroundColor: '#1C1C2E',
        borderRadius: 2,
    },
    progressSegmentActive: {
        backgroundColor: Colors.success,
    },
    titleSection: {
        paddingHorizontal: 24,
        marginTop: 40,
        alignItems: 'center',
    },
    headline: {
        color: '#FFFFFF',
        fontSize: 32,
        fontWeight: '900',
        textAlign: 'center',
    },
    subheadline: {
        color: Colors.textSecondary,
        fontSize: 15,
        marginTop: 12,
        lineHeight: 22,
        textAlign: 'center',
        paddingHorizontal: 20,
    },
    ratingSection: {
        alignItems: 'center',
        marginTop: 40,
    },
    svgWrapper: {
        alignItems: 'center',
        justifyContent: 'center',
    },
    ratingNumberBox: {
        position: 'absolute',
    },
    ratingNumber: {
        color: '#FFFFFF',
        fontSize: 64,
        fontWeight: '900',
    },
    starsRow: {
        flexDirection: 'row',
        gap: 16,
        marginTop: 30,
    },
    inputSection: {
        paddingHorizontal: 24,
        marginTop: 40,
    },
    inputLabel: {
        color: Colors.textSecondary,
        fontSize: 11,
        fontWeight: '900',
        letterSpacing: 1.5,
        marginBottom: 16,
    },
    textInputBox: {
        backgroundColor: Colors.card,
        borderRadius: 20,
        padding: 20,
        minHeight: 120,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.05)',
    },
    placeholderText: {
        color: 'rgba(255,255,255,0.2)',
        fontSize: 15,
    },
    footer: {
        padding: 24,
    },
    buttonText: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '700',
    },
});
