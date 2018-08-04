package com.yan.m3u8.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.yan.common.util.FileUtil;
import com.yan.m3u8.util.HttpsUtils;

/**
 * 了解下 m3u8
 * https://blog.csdn.net/blueboyhi/article/details/40107683
 * 
 * @author Yan
 *
 */
public class M3U8Downloader {

	public static void main(String[] args) throws Exception {
		
		// m3u8文件的url
		String m3u8Url = "";
    	
		// 当前应用处理的数据、中间结果、最终结果的根目录
		// workRootDirName目录下面包含3个目录：
		// m3u8存放m3u8文件
		// ts存放ts文件
		// mp4存放合并后的mp4文件
		String workRootDirName = "";
		
		// 下载线程数目
		int downloadThreadNum = 5;
		
		// 下载m3u8文件
		downloadM3U8(m3u8Url, workRootDirName, downloadThreadNum);
		
		// 合并根目录下所有的m3u8
//		mergeLocalTsFilesInWorkRootDir(workRootDirName);
		
	}

	/**
	 * 将workRoot文件夹下面所有的ts文件分别合并成mp4文件
	 * 
	 * @param workRootDirName
	 * @throws Exception 
	 */
	public static void mergeLocalTsFilesInWorkRootDir(String workRootDirName) throws Exception {
		File workRootDir = new File(workRootDirName);
		// 先从workRoot中读取文件夹，文件夹名即为fileName
		File[] fileDirs = workRootDir.listFiles(new FileFilter() {
			// 列出其中的文件夹
			public boolean accept(File pathname) {
				return pathname.isDirectory() && !pathname.getName().startsWith(".");
			}
		});
		
		if(fileDirs != null && fileDirs.length > 0) {
			// 序号，用于展示当前处理进度
			int index = 1;
			for(File fileDir:fileDirs) {
				String fileName = fileDir.getName();
				System.out.println("正在处理[" + fileName + "]，" + index + "/" + fileDirs.length);
				
				// m3u8文件
				String m3u8FileName = workRootDirName + "\\" + fileName + "\\m3u8\\" + fileName + ".m3u8";
				// ts文件的文件夹
				// 存放ts临时文件的地方
				String tsDirName = workRootDirName + "\\" + fileName + "\\ts";
				
				// 从m3u8文件中读取ts文件名（m3u8文件中文件名是排好序的）
				List<File> tsFiles = readSortedTsFilesFromM3U8File(m3u8FileName, tsDirName);
				
				// 合并ts文件
				mergeTsFile(workRootDirName, fileName, tsFiles);
				
				index++;
			}
		}
	}
	
	/**
	 * 从m3u8文件中读取出有序的ts文件列表
	 * 
	 * @param m3u8FileName m3u8文件
	 * @param tsDirName ts文件的文件夹
	 * @return
	 */
	private static List<File> readSortedTsFilesFromM3U8File(String m3u8FileName, String tsDirName){
		List<File> tsFiles = new ArrayList<>();
		File m3u8File = new File(m3u8FileName);
		Reader fileReader = null;
		
		BufferedReader bufferedReader = null;
		
		try {
			fileReader = new FileReader(m3u8File);
			bufferedReader = new BufferedReader(fileReader);
			
			String line = null;
			while((line = bufferedReader.readLine())!= null) {
				if(!line.startsWith("#")) {
					String tsFileName = tsDirName + "\\" + line;
					//System.out.println(tsFileName);
					File tsFile = new File(tsFileName);
					tsFiles.add(tsFile);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 关闭流
			if(bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		if(tsFiles.size() == 0) {
			tsFiles = null;
		}
		
		return tsFiles;
	}
	
	/**
	 * 下载m3u8文件到本地文件夹，并且将ts文件合并为mp4文件
	 * 
	 * @param m3u8Url
	 * @param workRootDirName
	 * @param downloadThreadNum 下载线程数目
	 * @throws Exception
	 */
	public static void downloadM3U8(String m3u8Url, String workRootDirName, int downloadThreadNum) throws Exception {
		File workRootDir = new File(workRootDirName);
		// 如果文件夹不存在创建文件夹
		if(!workRootDir.exists()) {
			workRootDir.mkdirs();
		}
		
		
		int index = m3u8Url.lastIndexOf("/");
		int index2 = m3u8Url.lastIndexOf(".m3u8");
		
		String urlPre = m3u8Url.substring(0, index+1);
		String fileName = m3u8Url.substring(index + 1, index2);
		File fileDir = new File(fileName);
		if(!fileDir.exists()) {
			fileDir.mkdirs();
		}
		
		// 存放m3u8文件的地方
		String m3u8DirName = workRootDirName + "\\" + fileName + "\\m3u8";
		File m3u8Dir = new File(m3u8DirName);
		if(!m3u8Dir.exists()) {
			m3u8Dir.mkdirs();
		}
		
		// 存放ts临时文件的地方
		String tsDirName = workRootDirName + "\\" + fileName + "\\ts";
		File tsDir = new File(tsDirName);
		if(!tsDir.exists()) {
			tsDir.mkdirs();
		}
		
		// 存放mp4文件的地方
		String mp4DirName = workRootDirName + "\\" + fileName + "\\mp4";
		File mp4Dir = new File(mp4DirName);
		if(!mp4Dir.exists()) {
			mp4Dir.mkdirs();
		}
		
		System.out.println("开始下载m3u8文件");
		// 下载m3u8文件
    	String result = HttpsUtils.get(m3u8Url, null);
    	
    	// 创建m3u8文件
    	String m3u8FileName = workRootDirName + "\\" + fileName + "\\m3u8\\" + fileName + ".m3u8";
		File m3u8File = new File(m3u8FileName);
		if(!m3u8File.exists()) {
			m3u8File.createNewFile();
		}
		// 将m3u9文件的内容写入文件
    	FileUtil.writeToFile(m3u8FileName, result, "UTF-8");
    	System.out.println("下载m3u8文件结束");
    	
    	// 从m3u8文件内容中获取ts文件的文件名
    	List<String> tsFileNames = new ArrayList<>();
    	String[] lines = result.split("[\\r\\n]");
    	for(String line: lines) {
    		if(!line.startsWith("#")) {
    			//System.out.println(line);
    			tsFileNames.add(line.replace("\r", "").replaceAll("\n", ""));
    		}
    	}
    	
    	// 定义一个闭锁
    	CountDownLatch countDownLatch = new CountDownLatch(tsFileNames.size());
    	// 创建下载线程
    	List<TsFileDownloadThread> tsFileDownloadThreads = new ArrayList<>();
    	for(int i=0;i<downloadThreadNum;i++) {
    		TsFileDownloadThread tsFileDownloadThread = new TsFileDownloadThread(i, workRootDirName, fileName, countDownLatch);
    		tsFileDownloadThreads.add(tsFileDownloadThread);
    	}
    	
    	
    	// 下载ts文件
    	// 下载好的ts文件的集合
    	List<File> tsFiles = new ArrayList<>();
    	
    	for(int i=0;i<tsFileNames.size();i++) {
    		String tsFileName = tsFileNames.get(i);
    		
    		String tsFileUrl = urlPre + tsFileName;
    		String fsFileFullName = workRootDirName + "\\" + fileName + "\\ts\\" + tsFileName;
    		File tsFile = new File(fsFileFullName);
    		if(!tsFile.exists()) {
    			tsFile.createNewFile();
    		}
    		
    		//System.out.println(tsFileUrl);
    		// 将ts文件的url分别添加到几个下载线程当中
    		int serialNo = i%downloadThreadNum;
    		TsFileDownloadThread tsFileDownloadThread = tsFileDownloadThreads.get(serialNo);
    		tsFileDownloadThread.addTsFileUrl(tsFileUrl);
    		
    		// 按照顺序收集tsFile的名称，避免后续合并ts文件顺序错乱
    		tsFiles.add(tsFile);
    	}
    	
    	// 启动下载ts文件的线程
    	for(TsFileDownloadThread tsFileDownloadThread:tsFileDownloadThreads) {
    		tsFileDownloadThread.start();
    	}
    	
    	// 等待下载线程下载结束，进行合并
    	countDownLatch.await();
    	
    	System.out.println("开始合并ts文件为一个mp4文件");
    	// 合并ts文件为一个mp4文件，需要注意ts文件的顺序
    	mergeTsFile(workRootDirName, fileName, tsFiles);
    	
    	System.out.println("下载[" + fileName + "]结束");
	}
	
	/**
	 * 根据ts文件的url下载ts文件
	 * @param url
	 * @param header
	 * @param outFile
	 * @return
	 * @throws Exception
	 */
	public static String downloadTsFile(String  url, Map<String, String> header, File outFile) throws Exception {
        String result = "";
        CloseableHttpClient httpClient = null;
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        
        try {
            httpClient = HttpsUtils.getHttpClient();
            HttpGet httpGet = new HttpGet(url);
            // 设置头信息
            if (header != null && header.size() > 0) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                	httpGet.addHeader(entry.getKey(), entry.getValue());
                }
            }
            
            HttpResponse httpResponse = httpClient.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = httpResponse.getEntity();
                inputStream = entity.getContent();
                fileOutputStream = new FileOutputStream(outFile);
                
                int len = 0;
				int size = 1024;
				byte[] bytes = new byte[size];
				while((len = inputStream.read(bytes)) != -1) {
					fileOutputStream.write(bytes, 0, len);
				}
				// 将文件输出到本地
				fileOutputStream.flush();
				
				EntityUtils.consume(entity);
            } else {
            	System.out.println(url + ":网络请求状态码不是200");
            }
        } catch (Exception e) {
        	throw e;
        } finally {
            if(fileOutputStream != null) {
            	fileOutputStream.close();
            }
        	
            if(inputStream != null) {
            	inputStream.close();
            }
        	
        	if (httpClient != null) {
                httpClient.close();
            }
        }
        return result;
    }
	
	/**
	 * 将多个ts文件合并为一个mp4文件
	 * 
	 * @param workRootDir
	 * @param fileName mp4文件的文件名（不带后缀名）
	 * @param tsFiles 按顺序排列好的ts文件
	 * @throws Exception
	 */
	public static void mergeTsFile(String workRootDir, String fileName, List<File> tsFiles) throws Exception {
		
		File mp4File = new File(workRootDir + "\\" + fileName + "\\mp4\\" + fileName + ".mp4");
		
		if(!mp4File.exists()) {
			mp4File.createNewFile();
		}
		FileOutputStream fileOutputStream = new FileOutputStream(mp4File);
		
		if(tsFiles != null) {
			for(File tmpFile : tsFiles) {
				FileInputStream fileInputStream = new FileInputStream(tmpFile);
				
				// 文件的大小单位是byte, 1k = 1024byte
				//System.out.println(fileInputStream.available());
				int len = 0;
				int size = 1024 * 1024;
				byte[] bytes = new byte[size];
				while((len = fileInputStream.read(bytes)) != -1) {
					fileOutputStream.write(bytes, 0, len);
				}
				
				fileInputStream.close();
			}
		}
		fileOutputStream.close();
	}
	
}

/**
 * 下载ts文件的线程
 * @author Yan
 *
 */
class TsFileDownloadThread extends Thread{

	private int serialNo;
	
	// 闭锁，用于下载ts线程和合并ts文件线程之间通信
	private CountDownLatch countDownLatch;
	
	private String workRootDirName;
	
	// 是要通过m3u8下载的文件的名称（不带后缀名的名称），不是ts文件的名称
	private String fileName;
	
	// 待下载的ts文件url
	private List<String> tsFileUrls;
	
	public TsFileDownloadThread(int serialNo, String workRootDirName, String fileName, CountDownLatch countDownLatch) {
		this.serialNo = serialNo;
		this.workRootDirName = workRootDirName;
		this.fileName = fileName;
		this.countDownLatch = countDownLatch;
		this.tsFileUrls = new ArrayList<>();
	}

	public void addTsFileUrl(String tsFileUrl) {
		this.tsFileUrls.add(tsFileUrl);
	}
	
	@Override
	public void run() {
		if(tsFileUrls != null) {
			for(String tsFileUrl : tsFileUrls) {
				
				// 从url中截取ts文件的名称
				int index = tsFileUrl.lastIndexOf("/");
				String tsFileName = tsFileUrl.substring(index + 1);
				
				// ts文件在本地的全路径名
				String fsFileFullName = workRootDirName + "\\" + fileName + "\\ts\\" + tsFileName;
	    		File tsFile = new File(fsFileFullName);
	    		// 如果ts文件在本地不存在，则创建
	    		if(!tsFile.exists()) {
	    			try {
						tsFile.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
	    		}
	    		
	    		// 下载ts文件
	    		try {
					M3U8Downloader.downloadTsFile(tsFileUrl, null, tsFile);
					System.out.println("TsFileDownloadThread-" + this.serialNo + ",下载ts文件结束:" + tsFileName);
				} catch (Exception e) {
					e.printStackTrace();
				}
	    		// 下载完ts文件要将计数器减一
	    		countDownLatch.countDown();
			}
		}
	}
	
}