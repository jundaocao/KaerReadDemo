package com.kaer.card.demo;

import com.kaer.sdk.IDCardItem;
import com.kaer.sdk.OnClientCallback;
import com.kaer.sdk.nfc.NfcReadClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class NFCReadActivity extends Activity implements OnClientCallback, OnClickListener {
	private NfcReadClient mNfcReadClient;
	private TextView message;
	private EditText ipEt, portEt;
	private Button setBtn;
	private ImageView photoIv;
	private NfcAdapter mAdapter;
	private long startTime;
	private ProgressBar proBar;
	private TextView proTv;
	private PowerManager pm;
	private WakeLock wl;
	// private int progress;
	private ReadAsync async;
	private int flag; // 读取方式
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 100) {
				proTv.setText(msg.arg1 + " %");
				proBar.setProgress(msg.arg1);
			}
			if (msg.what == 200) {
				updateResult((IDCardItem) msg.obj);
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nfc);
		initWidget();
		// 必须调用

		mNfcReadClient = NfcReadClient.getInstance(NFCReadActivity.this);
        if(!mNfcReadClient.checkNfcEnable(NFCReadActivity.this)){
			Toast.makeText(this, "不支持NFC或者未开启", Toast.LENGTH_SHORT).show();
		}
		mNfcReadClient.setClientCallback(this);
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "nfc");
		findViewById(R.id.clearBtn).setOnClickListener(this);

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		// 必须调用
		mAdapter = NfcAdapter.getDefaultAdapter(NFCReadActivity.this);

		if (mAdapter == null) {
			print("手机不支持NFC功能");
		} else if (!mAdapter.isEnabled()) {
			print("手机未打开nfc");
			new AlertDialog.Builder(NFCReadActivity.this).setTitle("是否打开NFC")
					.setPositiveButton("前往", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							startActivity(new Intent("android.settings.NFC_SETTINGS"));
						}
					}).setNegativeButton("否", null).create().show();
		} else
			mNfcReadClient.enableDispatch();
		if (wl != null && !wl.isHeld())
			wl.acquire();
	}

	private void releaseWakeLock() {
		if (wl != null && wl.isHeld()) {
			wl.release();
			wl = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		getMenuInflater().inflate(R.menu.menu_nfc, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		System.out.println("" + item.getTitle());
		switch (item.getItemId()) {
		case R.id.menu1:
			flag = 0;
			break;
		case R.id.menu2:
			flag = 1;
			break;
		case R.id.menu3:
			flag = 2;
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void initWidget() {
		ipEt = (EditText) findViewById(R.id.ipEt);
		portEt = (EditText) findViewById(R.id.portEt);
		setBtn = (Button) findViewById(R.id.setBtn);
		message = (TextView) findViewById(R.id.message);
		photoIv = (ImageView) findViewById(R.id.photo);

		proBar = (ProgressBar) findViewById(R.id.probar);
		proBar.setMax(100);
		proTv = (TextView) findViewById(R.id.proTv);
		setBtn.setOnClickListener(this);
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		clear();
		if (flag == 0) {
			// 主线程同步调用
			IDCardItem item = mNfcReadClient.readCardWithIntent(intent);
			updateResult(item);

		} else if (flag == 1) {

			// 子线程同步调用
			async = new ReadAsync();
			async.execute(intent);
		} else if (flag == 2) {
			// 异步调用
			mNfcReadClient.readCardWidhIntentAsync(intent);
		}
System.out.println("dfsfsfsfs");
	}

	public void print(String string) {
		String msg = message.getText().toString().trim();
		message.setText(msg + "\n" + string);
		Log.d("msg", msg);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mNfcReadClient.disableDispatch();
		releaseWakeLock();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		System.out.println("release");
		mNfcReadClient.release();
	}

	@Override
	public void readResult(IDCardItem arg0) {
		// TODO Auto-generated method stub
		mHandler.obtainMessage(200, arg0).sendToTarget();
	}

	private void updateResult(IDCardItem arg0) {
		if (arg0.retCode != 1) {
			clear();
		}
		switch (arg0.retCode) {
		case 1:
			updateView(arg0);
			break;
		case 2:
			print("身份证读取过程中移动");
			break;
		case 3:
			print("读取过程中网络异常");
			break;
		case 4:
			print("服务器连接失败");
			break;
		case 5:
			print("读取失败");
			break;
		case 6:
			print("标签类型不符合过滤条件");
			break;
		case 7:
			print("不支持的证件类型");
			break;
		case 8:
			print("读取超时");
			break;
		case 9:
			print("没有可用的解密设备");
			break;
		case 10:
			print("卡认证机具失败");
			break;
		case -1:
			print("重贴标签");
			break;
		case -2:
			print("未设置ip和端口");
			break;
		default:
			print("错误码:" + arg0.retCode);

			break;
		}
		print("读取共耗时:" + String.valueOf(System.currentTimeMillis() - startTime) + "毫秒");
	}

	private void updateView(IDCardItem item) {
		System.out.println("123");
		StringBuilder sb = new StringBuilder();
		sb.append("姓名:" + item.partyName + "\n");
		sb.append("性别:" + item.gender + "\n");
		sb.append("民族:" + item.nation + "\n");
		sb.append("出生:" + item.bornDay + "\n");
		sb.append("住址:" + item.certAddress + "\n");
		sb.append("公民身份证号:" + item.certNumber + "\n");
		sb.append("签发机关:" + item.certOrg + "\n");
		String effDate = item.effDate;
		String expDate = item.expDate;
		sb.append("有效期限:" + effDate.substring(0, 4) + "." + effDate.substring(4, 6) + "." + effDate.substring(6, 8)
				+ "-" + expDate.substring(0, 4) + "." + expDate.substring(4, 6) + "." + expDate.substring(6, 8) + "\n");
		print(sb.toString());
		photoIv.setImageBitmap(scale(item.picBitmap));
	}

	private Bitmap scale(Bitmap bitmap) {
		DisplayMetrics displaysMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaysMetrics);
		// TODO Auto-generated constructor stub
		int width = displaysMetrics.widthPixels;
		Matrix matrix = new Matrix();
		float scale = width / (4.0f * bitmap.getWidth());
		matrix.postScale(scale, scale); // 长和宽放大缩小的比例
		Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		return resizeBmp;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (proBar.getVisibility() == View.VISIBLE) {
				proBar.setVisibility(View.GONE);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void clear() {
		proBar.setProgress(0);
		proTv.setText(null);
		message.setText(null);
		photoIv.setImageBitmap(null);
	}

	@Override
	public void preExcute(long arg0) {
		// TODO Auto-generated method stub
		startTime = arg0;

	}

	@Override
	public void updateProgress(int arg0) {
		// TODO Auto-generated method stub
		System.out.println("arg0.progress=" + arg0);
		mHandler.obtainMessage(100, arg0, arg0).sendToTarget();
	}

	class ReadAsync extends AsyncTask<Intent, Integer, IDCardItem> {
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			clear();
			message.setText(null);
			photoIv.setImageBitmap(null);
		}

		@Override
		protected IDCardItem doInBackground(Intent... params) {
			// TODO Auto-generated method stub
			Intent intent = params[0];
			IDCardItem item = mNfcReadClient.readCardWithIntent(intent);
			return item;
		}

		@Override
		protected void onPostExecute(IDCardItem result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			readResult(result);
		}

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.setBtn) {
			String ip = ipEt.getText().toString().trim();
			String port = portEt.getText().toString().trim();
			if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
				Toast.makeText(this, "无效的ip或端口", Toast.LENGTH_SHORT).show();
				return;
			}
			mNfcReadClient.setServerAddr(ip, Integer.parseInt(port));
		} else if (v.getId() == R.id.clearBtn) {
			clear();
		}
	}

}
