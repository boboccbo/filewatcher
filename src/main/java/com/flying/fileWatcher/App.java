package com.flying.fileWatcher;

import java.io.*;
import java.net.ConnectException;

import com.artofsolving.jodconverter.DocumentConverter;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

/**
 * 
 * @author zdf
 * @Date 2017-11-20
 */
public class App {
	private static Logger log = Logger.getLogger(App.class);

	private static JSONObject propJsonObj = null;

	static {

		InputStream is = null;
		BufferedReader br = null;
		try {
			try{
				is  = new FileInputStream("config.json");

				log.debug("从当前文件读取config.json文件成功！");
			}catch (Exception e){
				log.error("从当前文件读取config.json文件失败！");

				is = App.class.getClassLoader().getResourceAsStream("config.json");

				log.debug("从配置文件路径读取config.json文件成功！");
			}


			br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			StringBuffer jsonStrBuff = new StringBuffer();
			String brStr = null;
			while ((brStr =br.readLine()) != null){
				jsonStrBuff.append(brStr);
			}
			propJsonObj = JSONObject.fromObject(jsonStrBuff.toString());
		} catch (UnsupportedEncodingException e) {
			log.error("配置文件config.json读取到不可解析的文字",e);
		} catch (IOException e) {
			log.error("读取config.json配置文件出错",e);
		}
	}

	public static void main(String[] args) throws Exception {
		if(propJsonObj.containsKey("watchFile")){
			JSONArray watchFileArray = propJsonObj.getJSONArray("watchFile");
			if(watchFileArray != null && watchFileArray.size()>0){
				for(int m=0;m<watchFileArray.size();m++) {
					final File file = new File(watchFileArray.getString(m));

					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								new WatchDir(file, true, new FileActionCallback() {
									@Override
									public void create(File file) {
										log.debug("文件已创建\t" + file.getAbsolutePath());
										try{
											if(file.isFile()){
												String fileName = file.getName();
												//String fileStem = fileName.substring(0,fileName.lastIndexOf("."));
												String fileSuffix = fileName.substring(fileName.lastIndexOf(".") + 1);
												JSONArray suffixs = propJsonObj.getJSONArray("watchFileSuffix");
												if(suffixs != null && suffixs.size()>0){
													for(int i=0;i<suffixs.size();i++){
														if(suffixs.getString(i).equals(fileSuffix)){
															File outputFile = new File(file.getAbsolutePath().substring(0,file.getAbsolutePath().lastIndexOf(".")) + ".pdf");
															if (outputFile.exists())
																outputFile.delete();
															outputFile.createNewFile();
															OpenOfficeConnection connection = new SocketOpenOfficeConnection(8100);
															try {
																connection.connect();
															} catch (ConnectException e) {
																log.error("文件转换出错，请检查OpenOffice服务是否启动。",e);
															}
															// convert
															DocumentConverter converter = new OpenOfficeDocumentConverter(connection);
															converter.convert(file, outputFile);
															connection.disconnect();
															break;
														}
													}
												}
											}else{
												log.debug(file.getName() + "不是一个文件");
											}
										}catch (IOException e){
											log.error("IO流出错" , e);
										}
									}

									@Override
									public void delete(File file) {
										log.debug("文件已删除\t" + file.getAbsolutePath());
										File outputFile = new File(file.getAbsolutePath().substring(0,file.getAbsolutePath().lastIndexOf(".")) + ".pdf");
										if (outputFile.exists())
											outputFile.delete();
									}

									@Override
									public void modify(File file) {
										log.debug("文件已修改\t" + file.getAbsolutePath());
									}
								});
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}).start();

					log.debug("正在监视文件夹:" + file.getAbsolutePath() + "的变化");
				}
			}
		}else{
			log.warn("config.json未设置watchFile，观察文件");
		}
	}
}