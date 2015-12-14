package model;

import java.io.File;
import javax.swing.filechooser.*;

public class CSVFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		if(f.isDirectory())
			return true;
		
		String extension = MyFileUtils.getExtension(f);
		return extension != null && extension.equals(MyFileUtils.CSV);
	}

	@Override
	public String getDescription() {
		return ".csv";
	}

}
