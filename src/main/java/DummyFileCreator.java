import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DummyFileCreator {
	private Path path;

	public void setPath(String path) throws IOException {
		this.path = Paths.get(path);
		
		if(!Files.exists(this.path)) {
			throw new IOException("Path is not exists.");
		}
	}
	
	public Path createDummyFile(String filename, int bytes) throws IOException {
		File file = Files.createFile(Paths.get(this.path.toString(), filename)).toFile();

		FileOutputStream fos = new FileOutputStream(file);
		fos.write(new byte[bytes]);
		fos.close();
		
		return Paths.get(file.toURI());
	}
	
	public List<Path> createDummyFiles(String filename, int bytes, int count) throws IOException {
		List<Path> list = new ArrayList<Path>(count);
		
		String pattern;
		StringBuilder builder = new StringBuilder();
		
		for(int i = 0 ; i < String.valueOf(count).length() ; i++) {
			builder.append("0");
		}
		pattern = builder.toString();
		
		DecimalFormat df = new DecimalFormat(pattern);

		for(int i = 0 ; i < count ; i++) {
			String sfx = df.format((long)i+1);
			list.add(this.createDummyFile(filename + sfx, bytes));			
		}
		
		return list;
	}
	
	public static void main(String[] args) {
		String baseDirectory = null;
		try {
			baseDirectory = Files.createTempDirectory("blobTest").toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(baseDirectory);
		
		DummyFileCreator dfc = new DummyFileCreator();
		
		List<Path> pathList = null;
		try {
			dfc.setPath(baseDirectory);
			pathList = dfc.createDummyFiles("cloudfile", 1024*1024, 10);
		} catch(IOException e) {
			e.printStackTrace();
		}
		if(pathList == null) return;
			
		for(Iterator<Path> itr = pathList.iterator(); itr.hasNext();) {
			System.out.println("File:" + itr.next().toString());
		}
		
		System.out.println("[End]");
	}
}
