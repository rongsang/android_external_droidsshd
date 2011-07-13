/**
 * 
 */
package tk.tanguy.droidsshd.system;

import tk.tanguy.droidsshd.R;
import tk.tanguy.droidsshd.tools.ShellSession;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * @author mestre
 *
 */
public class DroidSSHdService extends Service{ 

//	TODO - investigate about Service#startForeground();
//	http://developer.android.com/reference/android/app/Service.html#startForeground%28int,%20android.app.Notification%29

	private static final String TAG = "DroidSSHdService";

	private static NotificationManager mNotificationManager;
	private static FileObserver mPidWatchdog;

	private static int dropbearDaemonProcessId = 0;
	private static boolean dropbearDaemonRunning = false;

	private static boolean dropbearDaemonNotificationShown = false;

	private Handler serviceHandler = new Handler();
	
	// lock to handle (synchronized) FileObserver calls
	private static Object sLock = new Object();
	
	private static boolean serviceManualStartup = true;

	public boolean isDaemonRunning() {
		return dropbearDaemonRunning;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// TODO - implement further checks on Base.init for special cases 
		// TODO - i.e. this SVC was started on boot and DroidSSHd activity hasn't run just yet
		// TODO - (is it the same ctx? is it null? etc...) 
		Base.initialize(getBaseContext());
		serviceManualStartup = Base.manualServiceStart();
		mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		if (Base.debug) {
			Log.v(TAG, "onCreate called");
			if (serviceManualStartup) Log.d(TAG, "ManualServiceStart");
		}
	}

	// This is the old onStart method that will be called on the pre-2.0 platforms
	//#ifdef BEFORE_DONUT
	/*
	@Override
	public void onStart(Intent intent, int startId) {
		if (Base.debug) {
			Log.d(TAG, "onStart(" + intent.toString() + ", " + startId + ") called");
		}
		if (!Base.startDaemonAtBoot() && !Base.manualServiceStart()) {
			stopSelf();
			return;
		}
		handleStart(intent, 0, startId);
	}
	*/
	//#endif

	// This is the new onStart method
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		int sticky=Service.START_STICKY;
		serviceManualStartup = Base.manualServiceStart();
		if (!Base.startDaemonAtBoot() && !serviceManualStartup) {
			sticky=Service.START_NOT_STICKY;
		}
		handleStart(intent, flags, startId);
		return sticky;
	}
	
	private void handleStart(Intent intent, int flags, int startId) {
		setUpPidFileWatchdog(Base.getDropbearTmpDirPath());
		if (serviceHandler!=null) {
			serviceHandler.post(new updateDaemonStatus());
			if (!dropbearDaemonRunning) {
				serviceHandler.postDelayed(new startDropbear(), 200L);
				if (Base.debug) {
					Log.d(TAG, "handleStart called, starting dropbear daemon");
				}
			} else {
				if (Base.debug) {
					Log.d(TAG, "handleStart called, but dropbear is already running");
				}
			}
		} else {
			Log.e(TAG, "handleStart called, but serviceHandler is NULL!");
			handleStop();
		}
	}
	
	private void handleStop() {
		if (mPidWatchdog!=null) {
			mPidWatchdog.stopWatching();
			mPidWatchdog = null;
		}
		Base.setDropbearDaemonStatus(Base.DAEMON_STATUS_STOPPED);
		stopSelf();
	}

	@Override
	public void onDestroy() {
		handleStop();
		super.onDestroy();
	}
	
	protected class startDropbear implements Runnable {
		public void run(){
			if(Base.debug) {
				Log.d(TAG+"-startDropbear", "starting");
			}
			if (!dropbearDaemonRunning) {
				//ShellSession mDaemonSession = new ShellSession(TAG+"-daemon", Base.runDaemonAsRoot(), Base.debug, mLogviewHandler) {
				ShellSession mDaemonSession = new ShellSession(TAG+"-daemon", Base.runDaemonAsRoot(), Base.debug, null) {
					@Override 
					protected void onSessionReady() {
						if (Base.debug) {
							Log.d(tag, "onSessionReady called");
						}
						int uid;
						String authPublicKey=Base.getDropbearAuthorizedKeysFilePath();
						if (Base.runDaemonAsRoot()) {
							uid = 0;
							authPublicKey = Base.getDropbearDataDirPath() + "/.ssh/" + Base.DROPBEAR_AUTHORIZED_KEYS;
							try {
								cmd("[ -d '"+ Base.getDropbearDataDirPath() +"'] || mkdir -m 0754 '"+Base.getDropbearDataDirPath() + "/.ssh'");
							} finally {}
							try {
								cmd("cp '"+Base.getDropbearAuthorizedKeysFilePath()+"' '"+authPublicKey+"'");
							} finally {}
						} else {
							uid = android.os.Process.myUid();
						}

						//cm7.1 (0.53 + master pw merged)
						//cmd = String.format("%s -E -p %d -P %s -d %s -r %s -Y %s  # -A -U %s -G %s -N %s -R %s",

						cmd = String.format("%s -E -p %d -P %s -d %s -r %s -Y %s",

							Base.getDropbearBinDirPath() + "/" + Base.DROPBEAR_BIN_SRV,
							Base.getDaemonPort(),
							Base.getDropbearPidFilePath(),
							Base.getDropbearDssHostKeyFilePath(),
							Base.getDropbearRsaHostKeyFilePath(),
							Base.getPassword()
						);

						/* used in some dropbear forks
							uid,
							uid,
							Base.getUsername(),
							authPublicKey // dropbear 0.53 uses DROPBEAR_HOME /.ssh/authorized_keys
						*/

						if (Base.debug) {
							cmd = cmd + " -F >/tmp/dropbear.log 2>&1 &";
							Log.v(tag, "cmd = " + cmd);
						}
						cmd(cmd);
					}

					@Override
					protected void onStdOut(String line) {
						if(debug) {
							Log.d(tag, "'" + line + "'");
						}
						if (handler!=null) {
							Message message = new Message();
							Bundle data = new Bundle();
							data.putCharSequence("line", line + "\n");
							message.setData(data);
							handler.sendMessage(message);
						}
					}
					@Override
					protected void onStdErr(String line) { 
						onStdOut (line);
					}
				};
				mDaemonSession.start();
			} else {
				Log.i(TAG, "dropbear daemon already running");
			}
		}
	}

	private FileObserver createPidWatchdog(String path,int mask) {
//		TODO - perhaps move this object to Util and make it static
//		since we shall have at most one FileObserver active at any time 
		FileObserver observer = new FileObserver(path, mask) {
			@Override
			public void onEvent(int event, String path) {
				synchronized(sLock) {
				try {
					switch (event) {
					case FileObserver.CREATE:
						Base.setDropbearDaemonStatus(Base.DAEMON_STATUS_STARTING);
						if(Base.debug) {
							Log.v(TAG, "File " + path + " created");
							Log.d(TAG, "mCurrentStatus = Base.DAEMON_STATUS_STARTING");
						}
						break;
					case FileObserver.DELETE:
						Base.setDropbearDaemonStatus(Base.DAEMON_STATUS_STOPPED);
						hideNotification();
						if (Base.debug){
							Log.v(TAG, "File " + path + " deleted");
							Log.d(TAG, "mCurrentStatus = Base.DAEMON_STATUS_STOPPED");
						}
						break;
					case FileObserver.MODIFY:
						Base.setDropbearDaemonStatus(Base.DAEMON_STATUS_STARTED);
						showNotification();
						if (Base.debug){
							Log.v(TAG, "File " + path + " modified");
							Log.d(TAG, "mCurrentStatus = Base.DAEMON_STATUS_STARTED");
						}
						break;
					default:
						if (Base.debug){
							Log.v(TAG, "event = " + event + " path = " + path);
						}
						break;
					}
				} catch (Exception e) {
					Log.e(TAG, "Exception in createPidWatchdog", e);
					e.printStackTrace();
				}
				}
			}
		};
		return observer;
	}

	private void setUpPidFileWatchdog(String path) {
		int mask = FileObserver.CREATE + FileObserver.DELETE + FileObserver.MODIFY;
		if (mPidWatchdog!=null) {
			if (Base.debug) {
				Log.d(TAG, "setUpPidFileWatchdog called but PIDObserver has already been set-up");
			}
		} else {
			mPidWatchdog = createPidWatchdog(path, mask);
			mPidWatchdog.startWatching();
		}
		if (Base.debug) {
			Log.d(TAG, "PIDObserver.toString() = " + mPidWatchdog.toString());
			Log.v(TAG, "Watching " + path + " mask " + mask);
		}
	}

	protected void showNotification() {
		if (Base.isDropbearDaemonNotificationEnabled()) {
			Notification notifyDetails = new Notification(R.drawable.droidsshd_icon, getString(R.string.app_label), System.currentTimeMillis());
			Intent intent = new Intent();
			intent.setClass(Base.getContext(), tk.tanguy.droidsshd.DroidSSHd.class);
			intent.setAction(Intent.ACTION_DEFAULT);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
			notifyDetails.setLatestEventInfo(this, getString(R.string.app_label), "Dropbear listening on port " + Base.getDaemonPort(), pendingIntent);
			notifyDetails.flags |= Notification.FLAG_ONGOING_EVENT;
			notifyDetails.flags |= Notification.DEFAULT_SOUND;
			mNotificationManager.notify(R.string.app_label, notifyDetails);
			dropbearDaemonNotificationShown=true;
			if (Base.shouldAcquireWakeLock()) {
				Util.takeWakeLock();
			}
			if ((Base.shouldAcquireWifiLock()) && (Util.isWifiEnabled())) {
				Util.takeWifiLock();
			}
		}
	}

	protected void hideNotification() {
		if (dropbearDaemonNotificationShown) {
			mNotificationManager.cancel(R.string.app_label);
			dropbearDaemonNotificationShown=false;
			Util.releaseWakeLock();
			Util.releaseWifiLock();
		}
	}
	
	protected class updateDaemonStatus implements Runnable {
		public void run(){
			if(Base.debug) {
				Log.d(TAG+"-updateDaemonStatus", "started");
			}
			try {
				dropbearDaemonProcessId = Util.getDropbearPidFromPidFile(Base.getDropbearPidFilePath());
				if (dropbearDaemonProcessId!=0) {
					dropbearDaemonRunning = Util.isDropbearDaemonRunning();
				} else {
					dropbearDaemonRunning = false;
				}
				if(dropbearDaemonRunning) {
					Base.setDropbearDaemonStatus(Base.DAEMON_STATUS_STARTED);
					showNotification();
				} else {
					Base.setDropbearDaemonStatus(Base.DAEMON_STATUS_STOPPED);
					hideNotification();
				}
			} catch (Exception e) {
				Log.e(TAG, "Exception in updateDaemonStatus", e);
				e.printStackTrace();
				Base.setDropbearDaemonStatus(Base.DAEMON_STATUS_UNKNOWN);
			}
		}
	}

	private final IBinder mBinder = new DropbearDaemonHandlerBinder();
	
	public class DropbearDaemonHandlerBinder extends Binder {
		public DroidSSHdService getService() {
			return DroidSSHdService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

}
