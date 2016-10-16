package com.fangbaobao.upload.activity;


import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.utils.URLEncodedUtils;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fangbaobao.upload.R;
import com.fangbaobao.upload.constant.UrlUtil;
import com.fangbaobao.upload.util.UploadUtil;
import com.fangbaobao.upload.util.UploadUtil.OnUploadProcessListener;
/**
 * @author spring sky<br>
 * Email :vipa1888@163.com<br>
 * QQ: 840950105<br>
 * 说明：主要用于选择文件和上传文件操作
 */
public class MainActivity extends Activity implements OnClickListener,OnUploadProcessListener{
	private static final String TAG = "uploadImage";
	private String BASE_URL = "http://192.168.1.115:8080/Upload_Server";
	/**
	 * 去上传文件
	 */
	protected static final int TO_UPLOAD_FILE = 1;  
	/**
	 * 上传文件响应
	 */
	protected static final int UPLOAD_FILE_DONE = 2;
	/**
	 * 选择文件
	 */
	public static final int TO_SELECT_PHOTO = 3;
	/**
	 * 上传初始化
	 */
	private static final int UPLOAD_INIT_PROCESS = 4;
	/**
	 * 上传中
	 */
	private static final int UPLOAD_IN_PROCESS = 5;
	/***
	 * javaWeb服务器请求地址
	 */
	private static String requestURL = UrlUtil.BASE_URL+UrlUtil.UPLOAD_FILE_URL;
	/**
	 * 选择按钮和上传按钮
	 */
	private Button selectButton,uploadButton;
	/**
	 * 显示上传结果文本空间
	 */
	private TextView uploadImageResult;
	/**
	 * 进度条控件对象
	 */
	private ProgressBar progressBar;
	/**
	 * 图片路径集合
	 */
	private List<String> picPath = new ArrayList<String>();
	private ProgressDialog progressDialog;
	/**
	 * 显示上传图片用imageview对象
	 */
	private ImageView imageView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		initView();
	}

	/**
	 * 初始化数据
	 */
	private void initView() {
		selectButton = (Button) this.findViewById(R.id.selectImage);
		uploadButton = (Button) this.findViewById(R.id.uploadImage);
		selectButton.setOnClickListener(this);
		uploadButton.setOnClickListener(this);
		imageView = (ImageView) this.findViewById(R.id.imageView);
		uploadImageResult = (TextView) findViewById(R.id.uploadImageResult);
		progressDialog = new ProgressDialog(this);
		progressBar = (ProgressBar) findViewById(R.id.progressBar1);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.selectImage:
			Intent intent = new Intent(this,SelectPicActivity.class);
			startActivityForResult(intent, TO_SELECT_PHOTO);
			break;
		case R.id.uploadImage:
			if(picPath!=null)
			{
				handler.sendEmptyMessage(TO_UPLOAD_FILE);
			}else{
				Toast.makeText(this, "上传的文件路径为空", Toast.LENGTH_LONG).show();
			}
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode==Activity.RESULT_OK && requestCode == TO_SELECT_PHOTO)
		{
			String img_path = data.getStringExtra(SelectPicActivity.KEY_PHOTO_PATH);
			picPath.add(img_path);
			Bitmap bm = BitmapFactory.decodeFile(picPath.get(0));
			imageView.setImageBitmap(bm);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


	/**
	 * 上传服务器响应回调
	 */
	@Override
	public void onUploadDone(int responseCode, String message) {
		progressDialog.dismiss();
		Message msg = Message.obtain();
		msg.what = UPLOAD_FILE_DONE;
		msg.arg1 = responseCode;
		msg.obj = message;
		handler.sendMessage(msg);
	}

	private void toUploadFile(){
		uploadImageResult.setText("正在上传中...");
		progressDialog.setMessage("正在上传文件...");
		progressDialog.show();
		String fileKey = "pic";
		UploadUtil uploadUtil = UploadUtil.getInstance();;
		uploadUtil.setOnUploadProcessListener(this);  //设置监听器监听上传状态

		Map<String, String> params = new HashMap<String, String>();
		//user_info数据表字段：固定只有5个字段user_name,pass_word,gender,phone,mailbox
		params.put("user_name", "busixiaoqiang");
		params.put("pass_word", "123456");
		params.put("gender", "男");
		params.put("phone", "18613980569");
		params.put("mailbox", "284787574@qq.com");
		uploadUtil.uploadFile(picPath,fileKey, requestURL,params);
		//利用okhttp框架进行图片上传
		//new NetWorkImage().execute(picPath);
	}


	/**
	 * 开启异步线程进行图片等文件进行上传到服务器
	 * @author wsd_leigluoqiang
	 */
	class NetWorkImage extends AsyncTask<String, Void, String>{
		//执行过程中
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		//工作的子线程
		@Override
		protected String doInBackground(String... params) {
			return doPost(params[0]);
		}
		//工作线程完成之后的回调方法，在主线程进行
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
			System.out.println(result);
		}
	}
	/**
	 * 利用okhttp框架进行图片上传
	 * @param string
	 * @return 返回服务器上图片存储的真实路径
	 */
	public String doPost(String imagePath) {
		OkHttpClient mOkHttpClient = null;
		try {
			mOkHttpClient = new OkHttpClient();
		} catch (Exception e) {
		}
		String result = "error";  
		MultipartBody.Builder builder = new MultipartBody.Builder();
		builder.addFormDataPart("image", imagePath,  
				RequestBody.create(MediaType.parse("image/jpeg"), new File(imagePath)));  
		RequestBody requestBody = builder.build();  
		Request.Builder reqBuilder = new Request.Builder();  
		Request request = reqBuilder  
				.url(BASE_URL + "/uploadimage")  
				.post(requestBody)  
				.build();  

		Log.d(TAG, "请求地址 " + BASE_URL + "/uploadimage");  
		try{  
			Response response = mOkHttpClient.newCall(request).execute();  
			Log.d(TAG, "响应码 " + response.code());  
			if (response.isSuccessful()) {  
				String resultValue = response.body().string();  
				Log.d(TAG, "响应体 " + resultValue);  
				return resultValue;  
			}  
		} catch (Exception e) {  
			e.printStackTrace();  
		}  
		return result;  

	}


	@SuppressLint("HandlerLeak") 
	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case TO_UPLOAD_FILE:
				toUploadFile();
				break;

			case UPLOAD_INIT_PROCESS:
				progressBar.setMax(msg.arg1);
				break;
			case UPLOAD_IN_PROCESS:
				progressBar.setProgress(msg.arg1);
				break;
			case UPLOAD_FILE_DONE:
				String result = "响应码："+msg.arg1+"\n响应信息："+(String)msg.obj+"\n耗时："+UploadUtil.getRequestTime()+"秒";
				uploadImageResult.setText(result);
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}

	};

	@Override
	public void onUploadProcess(int uploadSize) {
		Message msg = Message.obtain();
		msg.what = UPLOAD_IN_PROCESS;
		msg.arg1 = uploadSize;
		handler.sendMessage(msg );
	}



	@Override
	public void initUpload(int fileSize) {
		Message msg = Message.obtain();
		msg.what = UPLOAD_INIT_PROCESS;
		msg.arg1 = fileSize;
		handler.sendMessage(msg );
	}

}