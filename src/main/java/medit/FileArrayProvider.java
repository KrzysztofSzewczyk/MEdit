package medit;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileArrayProvider {

	public String[] readLines(final String filename) throws IOException {
		final FileReader fileReader = new FileReader(filename);
		final BufferedReader bufferedReader = new BufferedReader(fileReader);
		final List<String> lines = new ArrayList<>();
		String line = null;
		while ((line = bufferedReader.readLine()) != null)
			lines.add(line);
		bufferedReader.close();
		return lines.toArray(new String[lines.size()]);
	}
}