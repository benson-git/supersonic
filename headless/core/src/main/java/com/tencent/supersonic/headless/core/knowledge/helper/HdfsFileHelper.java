package com.tencent.supersonic.headless.core.knowledge.helper;

import static com.hankcs.hanlp.HanLP.Config.CustomDictionaryPath;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.DynamicCustomDictionary;
import com.hankcs.hanlp.utility.Predefine;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Hdfs File Helper
 */
@Slf4j
public class HdfsFileHelper {

    /***
     * delete cache file
     * @param path
     * @throws IOException
     */
    public static void deleteCacheFile(String[] path) throws IOException {
        FileSystem fs = FileSystem.get(URI.create(path[0]), new Configuration());
        String cacheFilePath = path[0] + Predefine.BIN_EXT;
        log.info("delete cache file:{}", cacheFilePath);
        try {
            fs.delete(new Path(cacheFilePath), false);
        } catch (Exception e) {
            log.error("delete:" + cacheFilePath, e);
        }
        int customBase = cacheFilePath.lastIndexOf(FileHelper.FILE_SPILT);
        String customPath = cacheFilePath.substring(0, customBase) + FileHelper.FILE_SPILT + "*.bin";
        List<String> fileList = getFileList(fs, new Path(customPath));
        for (String file : fileList) {
            try {
                fs.delete(new Path(file), false);
                log.info("delete cache file:{}", file);
            } catch (Exception e) {
                log.error("delete " + file, e);
            }
        }
        log.info("fileList:{}", fileList);
    }

    /**
     * reset path
     *
     * @param customDictionary
     * @throws IOException
     */
    public static void resetCustomPath(DynamicCustomDictionary customDictionary) throws IOException {
        String[] path = HanLP.Config.CustomDictionaryPath;
        FileSystem fs = FileSystem.get(URI.create(path[0]), new Configuration());
        String cacheFilePath = path[0] + Predefine.BIN_EXT;
        int customBase = cacheFilePath.lastIndexOf(FileHelper.FILE_SPILT);
        String customPath = cacheFilePath.substring(0, customBase) + FileHelper.FILE_SPILT + "*.txt";
        log.info("customPath:{}", customPath);
        List<String> fileList = getFileList(fs, new Path(customPath));
        log.info("CustomDictionaryPath:{}", fileList);
        CustomDictionaryPath = fileList.toArray(new String[0]);
        customDictionary.path = (CustomDictionaryPath == null || CustomDictionaryPath.length == 0) ? path
                : CustomDictionaryPath;
        if (CustomDictionaryPath == null || CustomDictionaryPath.length == 0) {
            CustomDictionaryPath = path;
        }
    }

    public static List<String> getFileList(FileSystem fs, Path folderPath) throws IOException {
        List<String> paths = new ArrayList();
        FileStatus[] fileStatuses = fs.globStatus(folderPath);
        for (int i = 0; i < fileStatuses.length; i++) {
            FileStatus fileStatus = fileStatuses[i];
            paths.add(fileStatus.getPath().toString());
        }
        return paths;
    }
}
