package com.fangbaobao.upload.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.util.Log;

/**
 * 
 * 上传工具类
 * @author spring sky<br>
 * Email :vipa1888@163.com<br>
 * QQ: 840950105<br>
 * 支持上传文件和参数
 */
public class UploadUtil {
	private static UploadUtil uploadUtil;
	private static final String BOUNDARY =  UUID.randomUUID().toString(); // 边界标识 随机生成
	private static final String PREFIX = "--";
	private static final String LINE_END = "\r\n";
	private static final String CONTENT_TYPE = "multipart/form-data"; // 内容类型
	private UploadUtil() {

	}

	/**
	 * 单例模式获取上传工具类
	 * @return
	 */
	public static UploadUtil getInstance() {
		if (null == uploadUtil) {
			uploadUtil = new UploadUtil();
		}
		return uploadUtil;
	}

	private static final String TAG = "UploadUtil";
	private int readTimeOut = 10 * 1000; // 读取超时
	private int connectTimeout = 10 * 1000; // 超时时间
	/***
	 * 请求使用多长时间
	 */
	private static int requestTime = 0;

	private static final String CHARSET = "utf-8"; // 设置编码

	/***
	 * 上传成功
	 */
	public static final int UPLOAD_SUCCESS_CODE = 1;
	/**
	 * 文件不存在
	 */
	public static final int UPLOAD_FILE_NOT_EXISTS_CODE = 2;
	/**
	 * 服务器出错
	 */
	public static final int UPLOAD_SERVER_ERROR_CODE = 3;
	protected static final int WHAT_TO_UPLOAD = 1;
	protected static final int WHAT_UPLOAD_DONE = 2;

	/**
	 * android上传文件到服务器
	 * 
	 * @param filePath
	 *            需要上传的文件的路径
	 * @param fileKey
	 *            在网页上<input type=file name=xxx/> xxx就是这里的fileKey
	 * @param RequestURL
	 *            请求的URL
	 */
	public void uploadFile(List<String> filePath, String fileKey, String RequestURL,
			Map<String, String> param) {
		if (filePath == null) {
			sendMessage(UPLOAD_FILE_NOT_EXISTS_CODE,"文件不存在");
			return;
		}
		try {
			List<File> file = new ArrayList<File>();
			for(String file_path:filePath){
				file.add(new File(file_path));
			}
			uploadFiles(file, fileKey, RequestURL, param);
		} catch (Exception e) {
			sendMessage(UPLOAD_FILE_NOT_EXISTS_CODE,"文件不存在");
			e.printStackTrace();
			return;
		}
	}

	/**
	 * android上传文件到服务器
	 * 
	 * @param file
	 *            需要上传的文件
	 * @param fileKey
	 *            在网页上<input type=file name=xxx/> xxx就是这里的fileKey
	 * @param RequestURL
	 *            请求的URL
	 */
	public void uploadFiles(final List<File> file, final String fileKey,
			final String RequestURL, final Map<String, String> param) {
		if (file == null || file.size()==0) {
			sendMessage(UPLOAD_FILE_NOT_EXISTS_CODE,"文件不存在");
			return;
		}
		//开启线程上传文件
		new Thread(new Runnable() {  
			@Override
			public void run() {
				toUploadFile(file, fileKey, RequestURL, param);
			}
		}).start();
	}
	/**
	 * 自定义方法：批量上传参数数据和图片等文件数据
	 * @param files：File对象集合
	 * @param fileKey：相当于字段名，供服务器获取对应数据使用
	 * @param RequestURL：javaWeb服务器请求地址
	 * @param param：参数数据集合
	 */
	private void toUploadFile(List<File> files, String fileKey, String RequestURL,
			Map<String, String> param) {
		String result = null;
		requestTime= 0;

		long requestTime = System.currentTimeMillis();
		long responseTime = 0;

		try {
			URL url = new URL(RequestURL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(readTimeOut);
			conn.setConnectTimeout(connectTimeout);
			conn.setDoInput(true); // 允许输入流
			conn.setDoOutput(true); // 允许输出流
			conn.setUseCaches(false); // 不允许使用缓存
			conn.setRequestMethod("POST"); // 请求方式
//			conn.setRequestProperty("Charset", CHARSET); // 设置编码
			conn.setRequestProperty("connection", "keep-alive");
//			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
			conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary=" + BOUNDARY);
			conn.connect();

			/**
			 * 当文件不为空，把文件包装并且上传
			 */
			DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
			StringBuffer sb = null;
			String params = "";

			/***
			 * 以下是用于上传参数，可以一次性传递多个参数
			 * 实现方法：通过数据集合的迭代器，将数据一次写入输出流中
			 */
			if (param != null && param.size() > 0) {
				Iterator<String> it = param.keySet().iterator();
				while (it.hasNext()) {
					sb = null;
					sb = new StringBuffer();
					String key = it.next();
					String value = param.get(key);
					sb.append(PREFIX).append(BOUNDARY).append(LINE_END);
					sb.append("Content-Disposition: form-data; name=\"").append(key).append("\"").append(LINE_END).append(LINE_END);
					sb.append(value).append(LINE_END);
					params = sb.toString();
					Log.i(TAG, key+"="+params+"##");
					dos.write(params.getBytes());
				}
			}
			/**
			 * 将file对象集合写入输出流
			 * 做非空和长度判断，提高程序的性能
			 */
			if(files!=null&&files.size()>0){
				//int类型有可能范围不够
				int data_size = 0;
				//计算出数据的总长度
				for(File file:files){
					data_size += file.length();
				}
				//初始化上传监听，初始化准备上传数据的总长度
				onUploadProcessListener.initUpload(data_size);
				//一次将file对象写入输出流
				for(File file:files){
					//stringbuffer对象，作为拼接字符串对象
					sb = new StringBuffer();
					//字符串对象
					params = null;
					//将File对象转换成输入流，在这里值申明一个变量，减少重复对象的创建，节省内存
					InputStream is = null;
					/**
					 * 这里重点注意： name里面的值为服务器端需要key 只有这个key 才可以得到对应的文件
					 * filename是文件的名字，包含后缀名的 比如:abc.png
					 * 这里拼接的几个常量字符串，是固定的写法，作为服务器读取数据的标识作用
					 */
					sb.append(PREFIX).append(BOUNDARY).append(LINE_END);
					//设置数据字段
					sb.append("Content-Disposition:form-data; name=\"" + fileKey
							+ "\"; filename=\"" + file.getName() + "\"" + LINE_END);
					//这里配置的Content-type很重要的 ，用于服务器端辨别文件的类型的
					
					//删除了一个p字母
					
					sb.append("Content-Type:image/jpeg" + LINE_END); 
//					sb.append("Content-Type: application/octet-stream; charset=" + CHARSET + LINE_END);
					sb.append(LINE_END);
					params = sb.toString();
					Log.i(TAG, file.getName()+"=" + params+"##");
					dos.write(params.getBytes());
					//将File对象通过流的方式写入输出流
					is = new FileInputStream(file);
					//初始化byte[]对象，用于存放从输入流中读取的数据
					byte[] bytes = new byte[1024*8];
					//记录每次读取数据的字节长度
					int len = 0;
					//记录当前总共读取的数据长度
					int curLen = 0;
					//通过循环读取数据，返回值len代表读取的字节长度，读取失败时候，返回值为-1
					while ((len = is.read(bytes)) != -1) {
						//累计记录当前所有读取的数据长度
						curLen += len;
						//以bytes数组的形式向输出流写入数据，必须指定开始位置和数据总长度，不然会出错
						dos.write(bytes, 0, len);
						//及时更新数据上传进度
						onUploadProcessListener.onUploadProcess(curLen);
					}
					is.close();
					dos.write(LINE_END.getBytes());
					byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END).getBytes();
					dos.write(end_data);
				}
				//冲刷输出数据流，将数据写入服务器中
				dos.flush();
				sb = null;
			}
			/**
			 * 获取响应码 200=成功 当响应成功，获取响应的流
			 */
			int res = conn.getResponseCode();
			responseTime = System.currentTimeMillis();
			this.requestTime = (int) ((responseTime-requestTime)/1000);
			Log.e(TAG, "response code:" + res);
			if (res == 200) {
				Log.e(TAG, "request success");
				InputStream input = conn.getInputStream();
				StringBuffer sb1 = new StringBuffer();
				//				int ss;
				//				while ((ss = input.read()) != -1) {
				//					sb1.append((char) ss);
				//				}
				byte[] bytes = new byte[1024*8];
				int len = 0;
				while((len=input.read(bytes))!=-1){
					sb1.append(new String(bytes, 0, len, "utf-8"));
				}
				result = sb1.toString();
				Log.e(TAG, "result : " + result);
				sendMessage(UPLOAD_SUCCESS_CODE, result);
				return;
			} else {
				Log.e(TAG, "request error");
				sendMessage(UPLOAD_SERVER_ERROR_CODE,"上传失败：code=" + res);
				return;
			}
		} catch (MalformedURLException e) {
			sendMessage(UPLOAD_SERVER_ERROR_CODE,"上传失败：error=" + e.getMessage());
			e.printStackTrace();
			return;
		} catch (IOException e) {
			sendMessage(UPLOAD_SERVER_ERROR_CODE,"上传失败：error=" + e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	/**
	 * 发送上传结果
	 * @param responseCode
	 * @param responseMessage
	 */
	private void sendMessage(int responseCode,String responseMessage)
	{
		onUploadProcessListener.onUploadDone(responseCode, responseMessage);
	}

	/**
	 * 下面是一个自定义的回调函数，用到回调上传文件是否完成
	 * 
	 * @author shimingzheng
	 * 
	 */
	public static interface OnUploadProcessListener {
		/**
		 * 上传响应
		 * @param responseCode
		 * @param message
		 */
		void onUploadDone(int responseCode, String message);
		/**
		 * 上传中
		 * @param uploadSize
		 */
		void onUploadProcess(int uploadSize);
		/**
		 * 准备上传
		 * @param fileSize
		 */
		void initUpload(int fileSize);
	}
	private OnUploadProcessListener onUploadProcessListener;



	public void setOnUploadProcessListener(
			OnUploadProcessListener onUploadProcessListener) {
		this.onUploadProcessListener = onUploadProcessListener;
	}

	public int getReadTimeOut() {
		return readTimeOut;
	}

	public void setReadTimeOut(int readTimeOut) {
		this.readTimeOut = readTimeOut;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}
	/**
	 * 获取上传使用的时间
	 * @return
	 */
	public static int getRequestTime() {
		return requestTime;
	}

	public static interface uploadProcessListener{

	}




}
