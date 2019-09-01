import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudAppendBlob;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;

/**
 * 旧SDKによる削除処理の時間比較
 * @author yusuke.suzuki@microsoft.com
 *
 */
public class Main {

	public static void main(String[] args) {
		long stime, etime; // 時間計測用（開始ミリ秒、終了ミリ秒）
		String connectionString = null; // 接続文字列
		String containerName = null; // コンテナ名
		
		System.out.println("Azure Blob Storage bulk delete sample");
		
		System.out.println("Create Storage Account Instance with Connection String.");
		
		// プロパティ読み込み
		try(InputStream is = Main.class.getResourceAsStream("config.properties")) {

			Properties prop = new Properties();
			prop.load(is);

			connectionString = prop.getProperty("connection.string"); // 接続文字列
			containerName = prop.getProperty("container.name"); // コンテナ名
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		System.out.printf("connectionString:%s\n", connectionString);
		stime = Calendar.getInstance().getTimeInMillis();
		
		// ストレージアカウントインスタンスの作成
		CloudStorageAccount account;
		try {
			account = CloudStorageAccount.parse(connectionString);
		} catch (InvalidKeyException|URISyntaxException e) {
			e.printStackTrace();
			return;
		}
		
		CloudBlobClient client = account.createCloudBlobClient();
		try {

			// BLOBコンテナインスタンスの取得
			CloudBlobContainer container = client.getContainerReference(containerName);

			etime = Calendar.getInstance().getTimeInMillis();
			System.out.println("[Connectted]:"+ (etime -stime) + "ms"); // 接続までの所要時間
			
			// 現在クラウド上に存在するファイルのリスト表示
			for(Iterator<ListBlobItem> itr = container.listBlobs().iterator(); itr.hasNext();) {
				ListBlobItem itm = itr.next();
				System.out.printf("[File]:\t%s\n", itm.getUri().getPath());
				
			}

			// CreateDummyFile
			DummyFileCreator dfc = new DummyFileCreator();
			Path localPath = null;
			try {
				dfc.setPath(Files.createTempDirectory("blobtest").toString());
				localPath = dfc.createDummyFile("testfile", 1024*1024);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		
			// UPload Blob File
			stime = Calendar.getInstance().getTimeInMillis();
			CloudAppendBlob ablob = container.getAppendBlobReference("cloudfile");
			ablob.uploadFromFile(localPath.toString());
			etime = Calendar.getInstance().getTimeInMillis();
			System.out.println("[Upload]:" + (etime-stime) + "ms");

			// Delete Blob File
			stime = Calendar.getInstance().getTimeInMillis();
			CloudBlob dblob = container.getBlobReferenceFromServer("cloudfile");
			dblob.deleteIfExists();
			etime = Calendar.getInstance().getTimeInMillis();
			System.out.println("[Delete]:" + (etime-stime) + "ms");
			
			// Multiple Upload Blob File
			stime = Calendar.getInstance().getTimeInMillis();
			int count = 20;
			DecimalFormat df = new DecimalFormat("000");
			for(int i = 0 ; i < count ; i++) {
				ablob = container.getAppendBlobReference("cloudfile" + df.format((long)i+1));
				ablob.uploadFromFile(localPath.toString());
			}
			etime = Calendar.getInstance().getTimeInMillis();
			System.out.println("[Upload(Multi)]:" + (etime-stime) + "ms");

			// Multiple Delete Blob File
			stime = Calendar.getInstance().getTimeInMillis();
			for(int i = 0 ; i < count ; i++) {
				dblob = container.getBlobReferenceFromServer("cloudfile" + df.format((long)i+1));
				dblob.deleteIfExists();
			}
			etime = Calendar.getInstance().getTimeInMillis();
			System.out.println("[Delete(Multi)]:" + (etime-stime) + "ms");

			// Multiple Upload Blob File(Stream　APIによる非同期削除用に再アップロード）
			stime = Calendar.getInstance().getTimeInMillis();
			for(int i = 0 ; i < count ; i++) {
				ablob = container.getAppendBlobReference("cloudfile" + df.format((long)i+1));
				ablob.uploadFromFile(localPath.toString());
			}
			etime = Calendar.getInstance().getTimeInMillis();
			System.out.println("[Upload(Multi)]:" + (etime-stime) + "ms");
			
			List<Integer> list = new ArrayList<>(count);
			for(int i = 0 ; i < count ; i++) {
				list.add(i);
			}

			// Multiple Delete Blob File with Stream API（Stream APIを使用した非同期削除）
			stime = Calendar.getInstance().getTimeInMillis();
			list.parallelStream().forEach((i) ->{
				try {
					CloudBlob dblobl = container.getBlobReferenceFromServer("cloudfile" + df.format((long)i+1));
					dblobl.deleteIfExists();
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (StorageException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			etime = Calendar.getInstance().getTimeInMillis();
			System.out.println("[Delete(Multi/Stream)]:" + (etime-stime) + "ms");
			

		} catch (URISyntaxException | StorageException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
