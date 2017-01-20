/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.settings.applications;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.lang.reflect.Field;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PictureInPictureSettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    private FakeFeatureFactory mFeatureFactory;
    private PictureInPictureSettings mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mFragment = new PictureInPictureSettings();
    }

    @Test
    public void testIgnoredApp() {
        for (String ignoredPackage : mFragment.IGNORE_PACKAGE_LIST) {
            assertThat(checkPackageHasPictureInPictureActivities(ignoredPackage, true))
                            .isFalse();
        }
    }

    @Test
    public void testNonPippableApp() {
        assertThat(checkPackageHasPictureInPictureActivities("com.android.dummypackage")).isFalse();
        assertThat(checkPackageHasPictureInPictureActivities("com.android.dummypackage",
                false, false, false)).isFalse();
    }

    @Test
    public void testPippableApp() {
        assertThat(checkPackageHasPictureInPictureActivities("com.android.dummypackage",
                true)).isTrue();
        assertThat(checkPackageHasPictureInPictureActivities("com.android.dummypackage",
                false, true)).isTrue();
        assertThat(checkPackageHasPictureInPictureActivities("com.android.dummypackage",
                true, false)).isTrue();
    }

    @Test
    public void logSpecialPermissionChange() {
        mFragment.logSpecialPermissionChange(true, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_PICTURE_IN_PICTURE_ON_HIDE_ALLOW), eq("app"));

        mFragment.logSpecialPermissionChange(false, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_PICTURE_IN_PICTURE_ON_HIDE_DENY), eq("app"));
    }

    private boolean checkPackageHasPictureInPictureActivities(String packageName,
            boolean... resizeableActivityState) {
        ActivityInfoWrapper[] activities = null;
        if (resizeableActivityState.length > 0) {
            activities = new ActivityInfoWrapper[resizeableActivityState.length];
            for (int i = 0; i < activities.length; i++) {
                activities[i] = new MockActivityInfo(resizeableActivityState[i]
                        ? ActivityInfo.RESIZE_MODE_RESIZEABLE_AND_PIPABLE
                        : ActivityInfo.RESIZE_MODE_UNRESIZEABLE);
            }
        }
        return PictureInPictureSettings.checkPackageHasPictureInPictureActivities(packageName,
                activities);
    }

    private class MockActivityInfo implements ActivityInfoWrapper {

        private int mResizeMode;

        public MockActivityInfo(int resizeMode) {
            mResizeMode = resizeMode;
        }

        @Override
        public int getResizeMode() {
            return mResizeMode;
        }
    }
}
