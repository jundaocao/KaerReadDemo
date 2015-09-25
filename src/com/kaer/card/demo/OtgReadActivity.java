package com.kaer.card.demo;

import com.kaer.sdk.IDCardItem;
import com.kaer.sdk.OnClientCallback;
import com.kaer.sdk.otg.OtgReadClient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class OtgReadActivity extends Activity implements OnClientCallback, OnClickListener {
	private OtgReadClient mOtgReadClient;
	private TextView message;
	private EditText ipEt, portEt;
	private Button setBtn;
	private ImageView photoIv;
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
			} else if (msg.what == 200) {
				updateResult((IDCardItem) msg.obj);
			} else if (msg.what == 300) {
				print("读取异常＝" + msg.obj.toString());
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_otg);
		initWidget();
		// 必须调用

		mOtgReadClient = OtgReadClient.getInstance(OtgReadActivity.this);
		mOtgReadClient.setClientCallback(this);
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "nfc");
		findViewById(R.id.clearBtn).setOnClickListener(this);
		findViewById(R.id.readBtn).setOnClickListener(this);

	}
@Override
protected void onNewIntent(Intent intent) {
	// TODO Auto-generated method stub
	super.onNewIntent(intent);
System.out.println("onNewIntent");
}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (wl != null && !wl.isHeld())
			wl.acquire();
	}

	private void releaseWakeLock() {
		if (wl != null && wl.isHeld()) {
			wl.release();
			wl = null;
		}
	}

	//211.138.20.176

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

	public void print(String string) {
		String msg = message.getText().toString().trim();
		message.setText(msg + "\n" + string);
		Log.d("msg", msg);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		// mNfcReadClient.disableDispatch();
		releaseWakeLock();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mOtgReadClient.disconnectOtg();
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
			print("服务器连接成功，字节流打开失败");
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
			print("设备未连接");
			break;
		case -2:
			print("未设置ip和端口");
			break;
		case -3:
			print("读取中，请等待操作完成");
			break;
		case -4:
			print("不支持的设备");
			break;
		case -5:
			print("设备没有获取到许可");
			break;
		default:
			print("错误码:" + arg0.retCode);

			break;
		}
		print("读取共耗时:" + String.valueOf(System.currentTimeMillis() - startTime) + "毫秒");
	}

	private void updateView(IDCardItem item) {
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
			IDCardItem item = mOtgReadClient.readCardWithSync();
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
			mOtgReadClient.setServerAddr(ip, Integer.parseInt(port));
		} else if (v.getId() == R.id.clearBtn) {
			clear();
		} else if (v.getId() == R.id.readBtn) {
			try {
				// IDCardItem item = mOtgReadClient.readCardWithSync();
				// System.out.println(item.toString());
				// mHandler.obtainMessage(200, item).sendToTarget();
				clear();
				if (flag == 0) {
					// 主线程同步调用
					IDCardItem item = mOtgReadClient.readCardWithSync();
					updateResult(item);

				} else if (flag == 1) {

					// 子线程同步调用
					async = new ReadAsync();
					async.execute();
				}
			} catch (Exception e) {
				e.printStackTrace();
				mHandler.obtainMessage(300, e.getMessage()).sendToTarget();

			}
		}
	}

}
