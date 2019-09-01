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
 * ��SDK�ɂ��폜�����̎��Ԕ�r
 * @author yusuke.suzuki@microsoft.com
 *
 */
public class Main {

	public static void main(String[] args) {
		long stime, etime; // ���Ԍv���p�i�J�n�~���b�A�I���~���b�j
		String connectionString = null; // �ڑ�������
		String containerName = null; // �R���e�i��
		
		System.out.println("Azure Blob Storage bulk delete sample");
		
		System.out.println("Create Storage Account Instance with Connection String.");
		
		// �v���p�e�B�ǂݍ���
		try(InputStream is = Main.class.getResourceAsStream("config.properties")) {

			Properties prop = new Properties();
			prop.load(is);

			connectionString = prop.getProperty("connection.string"); // �ڑ�������
			containerName = prop.getProperty("container.name"); // �R���e�i��
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		System.out.printf("connectionString:%s\n", connectionString);
		stime = Calendar.getInstance().getTimeInMillis();
		
		// �X�g���[�W�A�J�E���g�C���X�^���X�̍쐬
		CloudStorageAccount account;
		try {
			account = CloudStorageAccount.parse(connectionString);
		} catch (InvalidKeyException|URISyntaxException e) {
			e.printStackTrace();
			return;
		}
		
		CloudBlobClient client = account.createCloudBlobClient();
		try {

			// BLOB�R���e�i�C���X�^���X�̎擾
			CloudBlobContainer container = client.getContainerReference(containerName);

			etime = Calendar.getInstance().getTimeInMillis();
			System.out.println("[Connectted]:"+ (etime -stime) + "ms"); // �ڑ��܂ł̏��v����
			
			// ���݃N���E�h��ɑ��݂���t�@�C���̃��X�g�\��
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

			// Multiple Upload Blob File(Stream�@API�ɂ��񓯊��폜�p�ɍăA�b�v���[�h�j
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

			// Multiple Delete Blob File with Stream API�iStream API���g�p�����񓯊��폜�j
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
