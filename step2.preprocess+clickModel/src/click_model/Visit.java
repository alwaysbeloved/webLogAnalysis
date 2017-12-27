package click_model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import beans.PageViewsBean;
import beans.VisitBean;

/**
 * ͨ��pageViews�����visit��
 * @author cbw
 *
 */
public class Visit {

	static class VisitMapper extends Mapper<LongWritable, Text, Text, PageViewsBean>{
		PageViewsBean pvBean = new PageViewsBean();
		Text k = new Text();
		@Override
		protected void map(LongWritable key, Text value,Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] fields = line.split("\001");
			int step = Integer.parseInt(fields[5]);
			pvBean.set(fields[0], fields[1], fields[2], fields[3],fields[4], step, fields[6], fields[7], fields[8], fields[9],fields[10]);
			k.set(pvBean.getSession());
			context.write(k, pvBean);
		}
	}
	
	static class VisitReducer extends Reducer<Text, PageViewsBean, NullWritable, VisitBean>{
		@Override
		protected void reduce(Text session, Iterable<PageViewsBean> pvBeans,Context context)
				throws IOException, InterruptedException {
			ArrayList<PageViewsBean> pvBeansList = new ArrayList<PageViewsBean>();
			for (PageViewsBean pvBean : pvBeans) {
				PageViewsBean bean = new PageViewsBean();
				try {
					BeanUtils.copyProperties(bean, pvBean);
					pvBeansList.add(bean);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			//����step����
			Collections.sort(pvBeansList, new Comparator<PageViewsBean>() {
				@Override
				public int compare(PageViewsBean o1, PageViewsBean o2) {
					
					return o1.getStep()>o2.getStep()?1:-1;
				}									
			});
			
			// ȡ���visit����βpageview��¼�������ݷ���VisitBean��
			VisitBean visitBean = new VisitBean();
			// ȡvisit���׼�¼
			visitBean.setInPage(pvBeansList.get(0).getRequest());
			visitBean.setInTime(pvBeansList.get(0).getTimestr());
			// ȡvisit��β��¼
			visitBean.setOutPage(pvBeansList.get(pvBeansList.size() - 1).getRequest());
			visitBean.setOutTime(pvBeansList.get(pvBeansList.size() - 1).getTimestr());
			// visit���ʵ�ҳ����
			visitBean.setPageVisits(pvBeansList.size());
			// �����ߵ�ip
			visitBean.setRemote_addr(pvBeansList.get(0).getRemote_addr());
			// ����visit��referal
			visitBean.setReferal(pvBeansList.get(0).getReferal());
			visitBean.setSession(session.toString());
			
			context.write(NullWritable.get(),visitBean);
		}
	}
	
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf);

		job.setJarByClass(Visit.class);

		job.setMapperClass(VisitMapper.class);
		job.setReducerClass(VisitReducer.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(PageViewsBean.class);

		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(VisitBean.class);
		
		
		/*FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));*/
		FileInputFormat.setInputPaths(job, new Path("E:/weblogs/pageviews"));
		FileOutputFormat.setOutputPath(job, new Path("E:/weblogs/visit"));
		
		boolean res = job.waitForCompletion(true);
		System.exit(res?0:1);

	}
}


















