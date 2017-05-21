package com.github.trytocatch.mapsearcher;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;



/**
 * 
 * @author trytocatch@163.com
 *
 */
public class Tester {

	public static GraphSearcher<Character> build(String str) {
		String[] datas = str.split(",\\s*");
		Character[] from = new Character[datas.length];
		Character[] to = new Character[datas.length];
		int[] weight = new int[datas.length];
		int n = 0;
		for (String s : datas) {
			from[n] = s.charAt(0);
			to[n] = s.charAt(1);
			weight[n] = Integer.parseInt(s.substring(2));
			n++;
		}
		return new GraphSearcher<Character>(from, to, weight);
	}

	public static String readFromFile(String inputPath) throws IOException {
		InputStream is = null;
		try {
			if(inputPath == null || inputPath.isEmpty())
				is = Tester.class.getResourceAsStream("/input2.txt");
			else
				is = new FileInputStream(inputPath);
			if(is == null)
				throw new FileNotFoundException("Can't find the input file.");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String str = br.readLine();
			if (str != null && !str.isEmpty()) {
				return str.replaceAll("^Graph:\\s*|^\\s+|\\s+$", "");
			}
			return "";
		} finally {
			if (is != null)
				is.close();
		}
	}

	public static void main(String[] args) {
		final GraphSearcher<Character> t;
		try {
			if (args.length == 1)
				t = build(readFromFile(args[0]));
			else
				t = build(readFromFile(null));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		int n = 0;
		MyTask[] tasks = new MyTask[] { new FixRoute('A', "ACDE"), new FixRoute('A', "ADCB"), new FixRoute('A', "ABDC"),
				new FixRoute('A', "AEBCDACDE"), new FixRoute('A', "AECED"),new RouteCounterWithMaxDepth('C', 'C', 5),
				new RouteCounterWithFixDepth('A', 'C', 5), new ShortestRoute('A', 'C'), new ShortestRoute('B', 'B'),
				new RouteCounterWithMaxDistance('D', 'C', 25)};
		long old = System.nanoTime();
		for (MyTask task : tasks) {
			n++;
			System.out.print("Output #"+n+": ");
			System.out.println(task.getSigleOutput(t.search(task)));
		}
//		final RouteCounterWithMaxDistance rcwmd = new RouteCounterWithMaxDistance('C', 'C', 35);
//		final Phaser p = new Phaser(5);
//		class Task2 extends Thread{
//			@Override
//			public void run() {
//				p.arriveAndAwaitAdvance();
//				System.out.println(rcwmd.getSigleOutput(t.search(rcwmd)));
//				p.arriveAndDeregister();
//			}
//		}
//		for(int m = 0;m<4;m++){
//			new Task2().start();
//		}
//		p.arriveAndAwaitAdvance();
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		t.search(new FixRoute('A', "AKOIULNKHGF"));
//		p.arriveAndAwaitAdvance();
//		System.out.println(rcwmd.getSigleOutput(t.search(rcwmd)));
		System.out.println((System.nanoTime() - old));
	}
	
	static interface ResultConvertor<R>{
		String getSigleOutput(R result);
	}
	
	static abstract class MyTask<N,R> extends Task<N, R> implements ResultConvertor<R>{

		public MyTask(N start) {
			super(start);
		}
	}
	
	/**
	 * get the distance of assigned route
	 */
	static class FixRoute extends MyTask<Character, int[]> {
		private String path;

		public FixRoute(Character start, String path) {
			super(start);
			this.path = path;
			setDepthFirst(false);
		}
		
		@Override
		protected ReturnState check(List<Character> steps, int depth, int weight, int[] result,
				Boolean isRepeated) {
			if (depth >= path.length() || !steps.get(depth).equals(path.charAt(depth)))
				return ReturnState.STOP;
			if (depth == path.length() - 1) {
				result[0] = weight;
				return ReturnState.BREAK;
			}
			return ReturnState.FORK_CONTINUE;
		}

		@Override
		public String getSigleOutput(int[] result) {
			if (result[0] == -1)
				return "NO SUCH ROUTE";
			else
				return String.valueOf(result[0]);
		}

		@Override
		public int[] createResultHolder() {
			return new int[]{-1};
		}
		
		@Override
		public ForkResultHandler<int[]> getForkResultHandler() {
			return IntArray2ResultHandler.instance;
		}
	}
	
	
	static class RouteCounterWithMaxDepth extends MyTask<Character, int[]> {
		char end;
		int maxDepth;

		public RouteCounterWithMaxDepth(Character start, char end, int maxDepth) {
			super(start);
			this.end = end;
			this.maxDepth = maxDepth;
			setDepthFirst(false);
		}

		@Override
		protected ReturnState check(List<Character> steps, int depth, int weight, int[] result,
				Boolean isRepeated) {
			if (depth > 0 && depth <= maxDepth && steps.get(depth).equals(end)) {
				result[0]++;
			}
			if (depth >= maxDepth)
				return ReturnState.STOP;
			else
				return ReturnState.CONTINUE;
		}

		@Override
		public String getSigleOutput(int[] result) {
			return String.valueOf(result[0]);
		}

		@Override
		public int[] createResultHolder() {
			return new int[]{0};
		}
		
		@Override
		public ForkResultHandler<int[]> getForkResultHandler() {
			return IntArrayResultHandler.instance;
		}
	}

	
	static class RouteCounterWithFixDepth extends MyTask<Character, int[]> {
		char end;
		int fixDepth;

		public RouteCounterWithFixDepth(Character start, char end, int fixDepth) {
			super(start);
			this.end = end;
			this.fixDepth = fixDepth;
			setDepthFirst(false);
		}

		@Override
		protected ReturnState check(List<Character> steps, int depth, int weight, int[] result,
				Boolean isRepeated) {
			if (depth == fixDepth && steps.get(depth).equals(end)) {
				result[0]++;
				return ReturnState.STOP;
			}
			if (depth >= fixDepth)
				return ReturnState.STOP;
			else
				return ReturnState.CONTINUE;
		}

		@Override
		public String getSigleOutput(int[] result) {
			return String.valueOf(result[0]);
		}

		@Override
		public int[] createResultHolder() {
			return new int[]{0};
		}
		
		@Override
		public ForkResultHandler<int[]> getForkResultHandler() {
			return IntArrayResultHandler.instance;
		}
	}


	static class ShortestRoute extends MyTask<Character, int[]> {
		char end;

		public ShortestRoute(Character start, char end) {
			super(start);
			this.end = end;
			setDepthFirst(false);
			setStopFurtherSearchOnRepeat(true);
		}

		@Override
		protected ReturnState check(List<Character> steps, int depth, int weight, int[] result,
				Boolean isRepeated) {
			if (depth > 0 && steps.get(depth).equals(end)) {
				if (result[0] ==-1 || result[0] > weight) {
					result[0] = weight;
				}
				return ReturnState.STOP;
			}
			if (result[0] !=-1 && weight >=result[0])
				return ReturnState.STOP;
			return ReturnState.CONTINUE;
		}

		@Override
		public String getSigleOutput(int[] result) {
			return String.valueOf(result[0]);
		}

		@Override
		public int[] createResultHolder() {
			return new int[]{-1};
		}
		
		@Override
		public ForkResultHandler<int[]> getForkResultHandler() {
			return IntArray2ResultHandler.instance;
		}
	}
	
	
	static class RouteCounterWithMaxDistance extends MyTask<Character, int[]> {
		char end;
		int maxDistance;
		int forkThreshold;

		public RouteCounterWithMaxDistance(Character start, char end, int maxDistance) {
			super(start);
			this.end = end;
			this.maxDistance = maxDistance;
			forkThreshold = maxDistance-(maxDistance>>>1);
			setDepthFirst(false);
			setMaxParallelTask(4000);
			setStopFurtherSearchOnRepeat(false);
		}

		@Override
		protected ReturnState check(List<Character> steps, int depth, int weight, int[] result,
				Boolean isRepeated) {
			if (weight >= maxDistance)
				return ReturnState.STOP;
			if (depth > 0 && steps.get(depth).equals(end)) {
				result[0]++;
			}
			if (weight == maxDistance - 1)
				return ReturnState.STOP;
//			return weight < forkThreshold?ReturnState.FORK_CONTINUE:ReturnState.CONTINUE;
//			return ReturnState.FORK_CONTINUE;
			return ReturnState.CONTINUE;
		}

		@Override
		public String getSigleOutput(int[] result) {
			return String.valueOf(result[0]);
		}

		@Override
		public int[] createResultHolder() {
			return new int[]{0};
		}
		
		@Override
		public ForkResultHandler<int[]> getForkResultHandler() {
			return IntArrayResultHandler.instance;
		}
	}
	static class IntArrayResultHandler implements ForkResultHandler<int[]>{
		static IntArrayResultHandler instance = new IntArrayResultHandler(); 
		@Override
		public int[] fork(int[] old) {
			return new int[]{0};
		}
		@Override
		public int[] merge(int[] result1, int[] result2) {
			result1[0] += result2[0];
			return result1;
		}
		@Override
		public boolean hasResult(int[] result) {
			return result[0]>0;
		}
	}
	static class IntArray2ResultHandler implements ForkResultHandler<int[]>{
		static IntArray2ResultHandler instance = new IntArray2ResultHandler();
		@Override
		public int[] fork(int[] old) {
			return new int[]{-1};
		}
		@Override
		public int[] merge(int[] result1, int[] result2) {
			if(result1[0] == -1)
				return result2;
			else if(result2[0] == -1)
				return result1;
			else
				return new int[]{result1[0]+result2[0]};
		}
		@Override
		public boolean hasResult(int[] result) {
			return result[0]>0;
		}
	}
}
