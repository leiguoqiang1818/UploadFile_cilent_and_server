package com.kevin.imageuploadserver;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.activation.FileDataSource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
/**
 * servlet类：处理图片和文件上传
 * response返回的是响应文件在服务器存储的相对路径
 */
public class UploadImageServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	/**
	 * map集合，用于封装文件数据的存放路径
	 */
	private java.util.Map<String,String> map_path = new HashMap<String,String>();
	/**
	 * map集合，用于封装基本字符串数据
	 */
	private java.util.Map<String,String> map_base = new HashMap<String,String>();
	/**
	 * 响应输出流
	 */
	private PrintWriter out;
	/**
	 * 响应客户端字符串
	 */
	private String message = "";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		//设置相应编码格式
		response.setCharacterEncoding("utf-8");
		//设置相应文本类型为html类型
		response.setContentType("text/html;charset=utf-8");
		//设置request对象的编码格式
//		request.setCharacterEncoding("utf-8");
		out = response.getWriter();
		//接收图片与用户Id等多种参数
		changeUserImage(request, response);
		//接收图片
		//uploadImage(request, response);
	}

	// 上传图片文件
	//	private void uploadImage(HttpServletRequest request, HttpServletResponse response) 
	//			throws ServletException, IOException {
	//		String message = "";
	//		try{
	//			DiskFileItemFactory dff = new DiskFileItemFactory();
	//			ServletFileUpload sfu = new ServletFileUpload(dff);
	//			List<FileItem> items = sfu.parseRequest(request);
	//			// 获取上传字段
	//			FileItem fileItem = items.get(0);
	//			// 更改文件名为唯一的
	//			String filename = fileItem.getName();
	//			if (filename != null) {
	//				filename = IdGenertor.generateGUID() + "." + FilenameUtils.getExtension(filename);
	//			}
	//			// 生成图片基本存储根路径
	//			String storeDirectory = getServletContext().getRealPath("/files/images");
	//			File file = new File(storeDirectory);
	//			//创建file对象，并且创建相应的存储文件夹
	//			if (!file.exists()) {
	//				file.mkdir();
	//			}
	//			//创建图片文件具体的分类存储路径，因为图片资源很多的话，访问速度会受到限制，故通过哈希算法做个基本的文件夹分类
	//			String path = genericPath(filename, storeDirectory);
	//			// 处理文件的上传
	//			try {
	//				//将图片文件写入服务器对应的路径中
	//				fileItem.write(new File(storeDirectory + path, filename));
	//				//返回图片在服务器中存储的相对路径，给客户端使用
	//				String filePath = "/files/images" + path + "/" + filename;
	//				message = filePath;
	//			} catch (Exception e) {
	//				message = "上传图片失败";
	//			}
	//		} catch (Exception e) {
	//			message = "上传图片失败";
	//		} finally {
	//			response.getWriter().write(message);
	//		}
	//	}

	// 修改用户的图片
	private void changeUserImage(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		map_base.clear();
		map_path.clear();
		try{
			//创建文件项目工厂对象
			DiskFileItemFactory dff = new DiskFileItemFactory();
			//创建文件上传组件对象
			ServletFileUpload sfu = new ServletFileUpload(dff);
			//利用上传组件进行request对象内容解析
			List<FileItem> items = sfu.parseRequest(request);
			/**
			 * 标记变量，上传成功数量，用于标记图片路径的对应顺序
			 */
			int Success_acount = 0;
			/**
			 * 标记变量，上传失败数量，用于标记图片路径的对应顺序
			 */
			int fail_acount = 0;
			for(FileItem item:items){
				//普通上传的参数
				if(item.isFormField()){
					String filedName = item.getFieldName();
					String filedValue = new String(item.getString().getBytes(), "utf-8");
					//封装数据
					map_base.put(filedName, filedValue);
					//文件类型参数
				} else {// 获取上传字段
					// 更改文件名为唯一的，避免以后上传图片的时候，会覆盖以前图片的现象
					String filename = item.getName();
					if (filename != null) {
						filename = IdGenertor.generateGUID() + "." + FilenameUtils.getExtension(filename);
					}
					// 生成存储路径
					String storeDirectory = getServletContext().getRealPath("/files/images");
					File file = new File(storeDirectory);
					if (!file.exists()) {
						file.mkdir();
					}
					//通过哈希算法在服务器创建真实的存储路径
					String path = genericPath(filename, storeDirectory);
					// 处理文件的上传
					try {
						//将文件写入服务器相关路径中
						item.write(new File(storeDirectory + path, filename));
						//返回图片服务器的相对地址
						String filePath = "/files/images" + path + "/" + filename;
						//封装 图片路径数据
						map_path.put("img_path_"+Success_acount, filePath);
						message = filePath;
						Success_acount++;
					} catch (Exception e) {
						fail_acount++;
						message = "上传图片失败数量："+fail_acount;
					}
				}
			}
			//循环解析出数据之后，将所有得到的数据添加到响应的数据库中
			handData();
		} catch (Exception e) {
			e.printStackTrace();
			//			message = "上传数据失败";
		} finally {
			out.write(message);
		}
	}
	/**
	 * 自定义方法：处理客户端传送过来的参数数据和文件上传服务器之后返回的相对路径
	 * 将这些数据全部添加到相应的数据表中
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws UnsupportedEncodingException 
	 */
	private void handData() throws InstantiationException,
	IllegalAccessException, ClassNotFoundException, SQLException, UnsupportedEncodingException {
		//加载sql server数据库引擎
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").newInstance();
		//初始化sql server对象相应数据库资源标识符
		String uri = "jdbc:sqlserver://localhost:1433;DatebaseName=fangbaobao";
		//连接数据库的登录名和密码
		String username = "sa";
		String password = "123456";
		//创建连接数据库连接对象
		Connection conn = DriverManager.getConnection(uri, username, password);
		//创建于数据库的会话对象，通过这个对象可以进行增，删，改，查操作
		Statement statement = conn.createStatement();
		//申明参数对应字符串
		String user_name = null;
		String pass_word = null;
		String gender = null;
		String phone = null;
		String mailbox = null;
		if(map_base.size()==5){
			//遍历map集中的所有数据，分别将数据添加到数据表中
			for(String key_string:map_base.keySet()){
				//user_info数据表字段：固定只有5个字段user_name,pass_word,gender,phone,mailbox
				String filed = map_base.get(key_string);
				if(key_string.equals("user_name")){
					user_name = filed;
				}else if(key_string.equals("pass_word")){
					pass_word = filed;
				}else if(key_string.equals("gender")){
					gender = filed;
				}else if(key_string.equals("phone")){
					phone = filed;
				}else{
					mailbox = filed;
				}
			}

			//user_info数据表字段：固定只有5个字段user_name,pass_word,gender,phone,mailbox
			//创建sql执行语句
			String sql = "insert into fangbaobao..user_info values('"+user_name+"','"+pass_word+
					"','"+gender+"','"+phone+"','"+mailbox+"')";
			message = sql;
			//执行数据语句，修改，添加，删除，都用该方法
			statement.executeUpdate(sql);
			message = "执行完sql语句";
		}

		//根据user_name将图片数据存放到另外另外一个图片数据表中，利用user_name号进行数据表之间的连接
		for(String key_string:map_path.keySet()){
			String sql_img = "insert into fangbaobao..image_info values('"+map_base.get("user_name")+"',"+null+","+null+",'"+map_path.get(key_string)+"')";
			//执行数据语句，修改，添加，删除，都用该方法
			statement.executeUpdate(sql_img);
		}
		message = "上传数据成功";
	}

	//计算文件的存放目录，通过哈希算法，得出不同分类的图片路径，为了降低图片的访问难度
	private String genericPath(String filename, String storeDirectory) {
		int hashCode = filename.hashCode();
		int dir1 = hashCode&0xf;
		int dir2 = (hashCode&0xf0)>>4;
		String dir = "/"+dir1+"/"+dir2;
		//在服务器上创建dir文件路径
		File file = new File(storeDirectory,dir);
		if(!file.exists()){
			file.mkdirs();
		}
		return dir;
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
