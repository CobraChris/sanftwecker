import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const java=n=>read('alarm/src/main/java/com/cobrachris/sanftwecker/'+n+'.java');

test('on-phone grants are always read from Android and not stored as app checkmarks',()=>{
  const status=java('SetupPermissionStatus');
  assert.match(status,/AlarmScheduler.canSchedule\(context\)/);
  assert.match(status,/checkSelfPermission\(Manifest.permission.POST_NOTIFICATIONS\)/);
  assert.match(status,/PackageManager.PERMISSION_GRANTED/);
  assert.match(status,/areNotificationsEnabled\(\)/);
  assert.match(status,/FullScreenPermissionStatus.isAllowed\(context\)/);
  assert.match(status,/getPackageManager\(\).canRequestPackageInstalls\(\)/);
  assert.match(status,/if\(!exactAlarms\(context\)\) missing\+\+/);
  assert.match(status,/if\(!notifications\(context\)\) missing\+\+/);
  assert.match(status,/if\(!fullScreen\(context\)\) missing\+\+/);
  assert.doesNotMatch(status,/SharedPreferences|edit\(\)|putBoolean/);
});

test('assistant always checks live on resume and opens Android-native grant screens',()=>{
  const activity=java('SetupAssistantActivity');
  const manifest=read('alarm/src/main/AndroidManifest.xml');
  assert.match(manifest,/<activity android:name="\.SetupAssistantActivity" android:exported="false"\/>/);
  assert.match(activity,/@Override public void onResume\(\)[\s\S]*?refreshPermissionStates\(\)/);
  assert.match(activity,/SetupPermissionStatus.missingRequired\(this\)/);
  assert.match(activity,/updateRow\(exactState,exactAction,SetupPermissionStatus.exactAlarms\(this\),false\)/);
  assert.match(activity,/SetupPermissionStatus.notifications\(this\),false/);
  assert.match(activity,/SetupPermissionStatus.fullScreen\(this\),false/);
  assert.match(activity,/SetupPermissionStatus.apkUpdates\(this\),true/);
  assert.match(activity,/Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM/);
  assert.match(activity,/Settings.ACTION_APP_NOTIFICATION_SETTINGS/);
  assert.match(activity,/requestPermissions\(new String\[\]\{Manifest.permission.POST_NOTIFICATIONS\}/);
  assert.match(activity,/Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT/);
  assert.match(activity,/Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES/);
  assert.match(activity,/Settings.ACTION_APPLICATION_DETAILS_SETTINGS/);
  assert.match(activity,/catch\(RuntimeException unsupported\)/);
  // No automatic permission prompts on app launch.
  const onCreate=activity.split('onCreate(Bundle state)')[1].split('onResume()')[0];
  assert.doesNotMatch(onCreate,/requestPermissions|openExactAlarmSettings|startActivity\(new Intent\(Settings/);
});

test('watch permissions are honestly marked as separately verified',()=>{
  const activity=java('SetupAssistantActivity');
  assert.match(activity,/Auf der Uhr prüfen – Status nicht vom Handy aus bestätigt/);
  assert.match(activity,/Die Uhr hat eigene Alarm- und Benachrichtigungsrechte/);
  assert.match(activity,/Öffne Sanftwecker Uhr direkt auf der Galaxy Watch/);
  assert.match(activity,/Alarmmeldungen erlauben/);
  assert.doesNotMatch(activity,/watchStatus.setText\("✓ Erlaubt"\)/);
});

test('alarm home keeps all original safety actions while moving banners to menu',()=>{
  const main=java('MainActivity');
  assert.match(main,/setupWarning.setTextColor\(0xFFFFBB66\)/);
  assert.match(main,/setupWarning.setOnClickListener\(v->openSetupAssistant\(\)\)/);
  assert.match(main,/SetupPermissionStatus.missingRequired\(this\)>0/);
  assert.match(main,/\+"Einrichtungsassistent"\)/);
  assert.match(main,/startActivity\(new Intent\(this,SetupAssistantActivity.class\)\)/);
  assert.match(main,/Alarm zwangsweise stoppen/);
  assert.match(main,/PhoneVibration.stop\(\)/);
  assert.match(main,/AlarmScheduler.cancelTest\(this\)/);
  assert.match(main,/RingingService.STOP/);
  assert.match(main,/LAUFENDEN WECKER ÖFFNEN/);
  assert.match(main,/schedulingWarning.setVisibility/);
  assert.doesNotMatch(main,/fullScreenPermissionButton|fullScreenPermissionStatus|refreshFullScreenPermissionUi/);
  assert.doesNotMatch(main,/base.addView\(fullScreenPermission/);
});
