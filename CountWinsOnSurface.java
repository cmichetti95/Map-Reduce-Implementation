/* Max Gruber -- December 2021
This program processes data from the World Tennis Association located at https://github.com/JeffSackmann/tennis_wta
The goal is to create the following data structure in HDFS (player id, surface, number of wins on surface)
 */
import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class ExperiencedSurface {

	// Map extends Mapper In Key In Val Out Key Out Val
	public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
		// Variables that store state across mappers
		// Create variables here to avoid creating new objects on each map
		private final static IntWritable num_one = new IntWritable(1);
		private Text emt_wrd = new Text();
		private String cur_str = new String();

		public void map(LongWritable cur_key, Text cur_val, Context cur_out) throws IOException, InterruptedException {
			String cur_lne = cur_val.toString();
			String[] cur_lst = cur_lne.split(",", 0);
	
			String winner = cur_lst[7];
			String surface = cur_lst[3];

			String emt_str = winner + "," + surface + ",";
			if ((emt_str.equals("") == false) && (emt_str.length() > 3)) {
				emt_wrd.set(emt_str);
				cur_out.write(emt_wrd, num_one);
			}

		}
	}

	public static class Combine extends Reducer<Text, IntWritable, Text, IntWritable> {

		public void reduce(Text cur_key, Iterable<IntWritable> cur_lst, Context cur_out)
				throws IOException, InterruptedException {
			int ths_sum = 0;
			for (IntWritable cur_val : cur_lst) {
				ths_sum += cur_val.get();
			}
			cur_out.write(cur_key, new IntWritable(ths_sum));
		}
	}

	public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {

		public void reduce(Text cur_key, Iterable<IntWritable> cur_lst, Context cur_out)
				throws IOException, InterruptedException {
			int ths_sum = 0;
			for (IntWritable cur_val : cur_lst) {
				ths_sum += cur_val.get();
			}
			/* Let's prevent printing small values: part 2 */
			cur_out.write(cur_key, new IntWritable(ths_sum));
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		/* Let's prevent printing small values: part 1 */
		conf.setInt("mincount", 100);
		Job job = new Job(conf, "countWins");

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		job.setMapperClass(Map.class);
		job.setCombinerClass(Combine.class);
		job.setReducerClass(Reduce.class);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setJarByClass(CountWins.class);

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		job.waitForCompletion(true);
	}

}