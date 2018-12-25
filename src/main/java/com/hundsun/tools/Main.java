package com.hundsun.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hundsun.match.ThostFtdcUserApiStruct;
import com.hundsun.module.StructModule;

import info.monitorenter.cpdetector.io.ASCIIDetector;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.ParsingDetector;
import info.monitorenter.cpdetector.io.UnicodeDetector;

/**
 * 替换文件指定内容
 * 
 * @author zhangyx25316
 *
 */
public class Main {
	
	private static ThostFtdcUserApiStruct thostFtdcUserApiStruct = null;
	
	public static void main(String[] args) {
		//String filePath = System.getProperty("user.dir") + "\\ctp_wrap.cxx";
		String filePath = "F:\\文件内容替换工具\\ctp_wrap.cxx";
		thostFtdcUserApiStruct = new ThostFtdcUserApiStruct();
		replaceFileContext(filePath);
	}
	
	/**
	 * 替换文件中特定内容
	 * 
	 * @param filePath 文件所在路径
	 */
	public static void replaceFileContext(String filePath) {
		File file = new File(filePath);
		FileInputStream fileInputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		PrintWriter printWriter = null;
		try {
			//获取文件编码
            String enCode = getFileEncode(file.getAbsolutePath());  
            if("void".equals(enCode)){  
            	enCode="UTF-8";  
            }
            if("windows-1252".equals(enCode)){  
                enCode="GBK";  
            }
			fileInputStream = new FileInputStream(file);
			inputStreamReader = new InputStreamReader(fileInputStream, enCode);
			bufferedReader = new BufferedReader(inputStreamReader);
			List<StructModule> moduleList = new ArrayList<>();
			StringBuffer stringBuffer = new StringBuffer();
			String headLine = "#include \"util.h\"";
			stringBuffer.append(headLine);
			//行与行之间的分割
            stringBuffer.append(System.getProperty("line.separator"));
			
			String line = null;
			while((line = bufferedReader.readLine()) != null) {
				String sign = "SWIGEXPORT jstring JNICALL Java_com_ctp_ctpJNI_";
				//匹配需要替换的行
				Pattern replacePattern = Pattern.compile(".*NewStringUTF.*");
				String replaceString = "";
				Matcher replaceMatcher = replacePattern.matcher(line);
				//匹配getter方法
				Pattern keyWordPattern = Pattern.compile("^" + sign + ".*");
				String keyWordString = "";
				Matcher keyWordMatcher = keyWordPattern.matcher(line);
				String structKeyWord = "";
				String fieldKeyWord = "";
				//遍历包含特定关键字的行
				while(keyWordMatcher.find()){
					StructModule structModule = new StructModule();
					keyWordString += keyWordMatcher.group(0);
					structKeyWord = keyWordString.split(sign)[1]
							 					 .split("_1")[0];
					if(keyWordString.split(sign)[1].split("_1").length > 2) {
						fieldKeyWord = keyWordString.split(sign)[1]
							 						.split("_1")[1];
					} else {
						fieldKeyWord = keyWordString.split(sign)[1]
													.split("_1")[1]
													.split("\\(")[0];
					}
					structModule.setKey(structKeyWord);
					structModule.setValue(fieldKeyWord);
					moduleList.add(structModule);
				}
				
				//遍历包含需要替换关键字的行
				while(replaceMatcher.find()){
					replaceString += replaceMatcher.group(0);
					Map<String, String> map = new LinkedHashMap<>();
					String replaceContext = "";
					int index = ThostFtdcUserApiStruct.specifyQuery(moduleList.get(moduleList.size() - 1).getKey(), 
											moduleList.get(moduleList.size() - 1).getValue());
					if(index != 0) {
						int capacity = index * 2 + 1;
						replaceContext = "  char result_utf8[" + capacity + "] = { 0 };\n" + 
								"  if (result) {\n" + 
								"    unsigned int inlength = strlen(result);\n" + 
								"    int rsp = code_convert(\"gb2312\", \"utf-8\", result, inlength, result_utf8, " + capacity + ");\n" + 
								"    if (rsp != -1) jresult = jenv->NewStringUTF((const char *)result_utf8);\n" + 
								"  }";
					} 
					map.put("NewStringUTF", replaceContext);
					for (Map.Entry<String, String> entry : map.entrySet()) {  
	                	//判断当前行是否存在想要替换掉的字符 -1表示存在
	                    if(line.indexOf(entry.getKey()) != -1 && index != 0){ 
	                    	//替换为你想替换的内容
	                        line = line.replace(replaceString, entry.getValue());
	                    }
	                }
				}
                stringBuffer.append(line);
                //行与行之间的分割
                stringBuffer.append(System.getProperty("line.separator"));
			}
			//String newFilePath = System.getProperty("user.dir") + "\\new_ctp_wrap.cxx";
			String newFilePath = "F:\\文件内容替换工具\\new_ctp_wrap.cxx";
			File newFile = new File(newFilePath);
			if(!newFile.exists()) {
				newFile.createNewFile();
			} 
			//替换后输出的文件位置
			printWriter = new PrintWriter(newFile);
            printWriter.write(stringBuffer.toString().toCharArray());
            printWriter.flush();
            
		}catch (FileNotFoundException e) {
        	e.printStackTrace();
        	System.out.println("文件不存在！");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("读取文件失败！");
        } catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(inputStreamReader != null) {
				try {
					inputStreamReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(printWriter != null) {
				printWriter.close();
			}
		}
	}
	
	/**
	 * 检查文件类型
	 * 
	 * @param filePath 文件所在路径
	 * @return 文件类型
	 */
	public static String getFileEncode(String filePath) {  
        /* 
         * detector是探测器，它把探测任务交给具体的探测实现类的实例完成。 
         * cpDetector内置了一些常用的探测实现类，这些探测实现类的实例可以通过add方法 加进来，如ParsingDetector、 
         * JChardetFacade、ASCIIDetector、UnicodeDetector。 
         * detector按照“谁最先返回非空的探测结果，就以该结果为准”的原则返回探测到的 
          * 字符集编码。使用需要用到三个第三方JAR包：antlr.jar、chardet.jar和cpdetector.jar 
         * cpDetector是基于统计学原理的，不保证完全正确。 
         */  
        CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();  
        /* 
         * ParsingDetector可用于检查HTML、XML等文件或字符流的编码,构造方法中的参数用于 
          * 指示是否显示探测过程的详细信息，为false不显示。 
         */  
        detector.add(new ParsingDetector(false));  
        /* 
         * JChardetFacade封装了由Mozilla组织提供的JChardet，它可以完成大多数文件的编码 
         * 测定。所以，一般有了这个探测器就可满足大多数项目的要求，如果你还不放心，可以 
         * 再多加几个探测器，比如下面的ASCIIDetector、UnicodeDetector等。 
         */  
        // ASCIIDetector用于ASCII编码测定  
        detector.add(ASCIIDetector.getInstance());  
        // UnicodeDetector用于Unicode家族编码的测定  
        detector.add(UnicodeDetector.getInstance());  
        java.nio.charset.Charset charset = null;  
        File f = new File(filePath);  
        try {  
            charset = detector.detectCodepage(f.toURI().toURL());  
        } catch (Exception ex) {  
            ex.printStackTrace();  
        }  
        if (charset != null)  
            return charset.name();  
        else  
            return null;  
    } 
}
