# Avatar Model Download Fix - Implementation Summary

## Problem Description
The avatar was not displaying on the interview screen. Instead of showing an animated DUIX avatar with lip-sync, users saw a blank or dark card. The root cause was that model files downloaded from the backend were corrupted or incomplete.

## Root Causes Identified

1. **No HTTP Redirect Following**: The OkHttpClient wasn't configured to automatically follow 307 redirects from the backend API
2. **No Fallback URLs**: If the backend endpoint failed, there was no fallback to direct GitHub/Supabase URLs
3. **Insufficient Validation**: Downloaded files weren't properly validated before extraction
4. **Poor Error Handling**: Limited retry logic without exponential backoff
5. **Incomplete Logging**: Difficult to diagnose what was failing during download

## Changes Implemented

### 1. Updated Download Strategy (`NativeDuixAvatarView.kt`)

**New URL Priority Order:**
```kotlin
// DIRECT URLs FIRST (Most Reliable)
1. Direct GitHub for base config: 
   https://github.com/duixcom/Duix-Mobile/releases/download/v1.0.0/gj_dh_res.zip

2. Direct Supabase for models (Sofia, Oliver, Lily):
   ${DUIX_MODEL_BASE_URL}/${modelName}.zip

3. Backend API as fallback:
   ${PREZZENCE_API_URL}/api/duix/models/download/${modelName}.zip
```

**HTTP Improvements:**
- Explicitly enabled `followRedirects(true)` and `followSslRedirects(true)`
- Added buffered streams for more reliable file downloads
- Proper handling of 404 responses (skip to next URL immediately)

**Enhanced Validation:**
- File size check (minimum 1000 bytes)
- ZIP signature verification (0x04034b50)
- Post-extraction content validation with detailed file listing

**Retry Logic:**
- Exponential backoff: 2 seconds, then 4 seconds
- Maximum 2 retries per URL
- Tries multiple URLs before failing

**Comprehensive Logging:**
- Prefixed with `>>>` for easy filtering
- Shows HTTP response codes and content types
- Logs bytes written vs content-length
- Lists actual files found vs expected files
- Clear SUCCESS/FAILED markers

### 2. Added BuildConfig Support (`app/build.gradle`)

```groovy
def duixModelBaseUrl = envOrProp('DUIX_MODEL_BASE_URL', '')
...
buildConfigField 'String', 'DEBUG_DUIX_MODEL_BASE_URL', "\"${escapeBuildConfig(duixModelBaseUrl)}\""
```

This allows configuring Supabase model storage via environment variable:
```bash
export DUIX_MODEL_BASE_URL="https://your-project.supabase.co/storage/v1/object/public/duix-models"
```

### 3. Added Cache Management

**New Function:**
```kotlin
fun clearModelCache(context: Context) {
    val root = modelRootFor(context)
    Log.i("PrezzenceDuix", "Clearing model cache at: ${root.absolutePath}")
    root.deleteRecursively()
    root.mkdirs()
    Log.i("PrezzenceDuix", "Model cache cleared successfully")
}
```

**UI Button in Device QA Screen:**
- Added "Clear Avatar Model Cache" button in Settings → Device QA
- Allows users to force fresh model downloads
- Shows success/error toasts with clear feedback

## Testing Instructions

### Step 1: Clear Existing Cache
1. Open the app
2. Navigate to: **Settings → Device QA**
3. Scroll to "Avatar Model Cache" section
4. Tap **"Clear Avatar Model Cache"**
5. You should see: "Model cache cleared. Models will re-download on next interview."

### Step 2: Start a New Interview
1. Go back to Home
2. Tap **"New Session"**
3. Choose any interview type
4. Select interviewer (Sofia, Oliver, or Lily)
5. Create the interview session

### Step 3: Monitor the Download
The enhanced logging will show:
```
>>> Downloading model Sofia (URL 1/2, attempt 1/2)
>>> From: https://[supabase-url]/Sofia.zip
>>> HTTP Response: 200 OK
>>> Downloading: 52428800 bytes, type: application/zip
>>> Wrote 52428800 bytes to disk
>>> Final file size: 52428800 bytes
>>> ZIP signature: 0x04034b50 (valid: true)
>>> SUCCESS: Model Sofia downloaded: 52428800 bytes
>>> Calling ZipUtil.unzip...
>>> ZipUtil.unzip returned: true
>>> Model Sofia validated successfully!
```

### Step 4: Verify Avatar Display
1. The interview screen should show the avatar card
2. You should see the avatar's face/body animated
3. When the question plays, the avatar's lips should sync with the speech
4. Toast messages will confirm: "Avatar model ready: Sofia"

## Expected File Sizes (Approximate)

- `gj_dh_res.zip` (base config): ~25-30 MB
- `Sofia.zip`: ~50-60 MB  
- `Oliver.zip`: ~50-60 MB
- `Lily.zip`: ~50-60 MB

## Troubleshooting

### If models still don't download:

1. **Check logcat for detailed error messages:**
   ```bash
   adb logcat | grep "PrezzenceDuix"
   ```

2. **Verify backend is running:**
   - Check `PREZZENCE_API_URL` in `build.gradle`
   - Test backend endpoint: `GET /api/duix/models/download/gj_dh_res.zip`

3. **Verify Supabase configuration:**
   - Set `DUIX_MODEL_BASE_URL` environment variable
   - Ensure models are uploaded to Supabase storage bucket
   - Check bucket permissions (public read access required)

4. **Network connectivity:**
   - Ensure device has internet access
   - Try on WiFi vs cellular
   - Check if GitHub/Supabase are accessible from device

### If avatar initializes but doesn't animate:

1. **Check DUIX SDK callbacks:**
   - Look for "DUIX initialized: Sofia" toast
   - Check for `CALLBACK_EVENT_INIT_READY` in logs

2. **Verify audio playback:**
   - TTS audio should be generated
   - Check for `onSpeechReady` callback

3. **Model file integrity:**
   - Clear cache and re-download
   - Check extracted files match expected structure

## Backend Requirements

### Environment Variables (`.env`):
```bash
DUIX_MODEL_BASE_URL=https://your-project.supabase.co/storage/v1/object/public/duix-models
DUIX_BASE_CONFIG_URL=https://github.com/duixcom/Duix-Mobile/releases/download/v1.0.0/gj_dh_res.zip
```

### Supabase Storage:
Upload these files to your `duix-models` bucket:
- `Sofia.zip`
- `Oliver.zip`
- `Lily.zip`

The `gj_dh_res.zip` file is automatically downloaded from GitHub (too large for Supabase free tier).

## Key Benefits of This Implementation

1. **Resilience**: Falls back to direct URLs if backend fails
2. **Reliability**: Proper redirect following and buffered streams
3. **Transparency**: Comprehensive logging for debugging
4. **User Control**: Cache clearing button for troubleshooting
5. **Validation**: Multiple layers of file integrity checks
6. **Retry Logic**: Exponential backoff prevents transient failures

## Files Modified

1. `app/src/main/java/com/pollecode/prezzencekotlin/nativebridge/NativeDuixAvatarView.kt`
   - Rewrote `downloadAndUnzipStatic()` function
   - Added `clearModelCache()` function
   - Enhanced logging throughout

2. `app/build.gradle`
   - Added `DUIX_MODEL_BASE_URL` support
   - Added `DEBUG_DUIX_MODEL_BASE_URL` BuildConfig field

3. `app/src/main/java/com/pollecode/prezzencekotlin/MainActivity.kt`
   - Added "Clear Avatar Model Cache" button in Device QA screen

## Next Steps

1. **Deploy the updated APK** to test devices
2. **Clear model cache** using the new button
3. **Monitor logs** during first interview after cache clear
4. **Verify avatar displays and animates** correctly
5. **Test with all three personas** (Sofia, Oliver, Lily)

If issues persist after these changes, the enhanced logging will show exactly which URL succeeded/failed and what files were extracted, making diagnosis much easier.
