package myfirstapp.ui;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import lucene_3_tika.IndexFile;
import myfirstapp.eventListener.Plugin;
import myfirstapp.sys.App;
import myfirstapp.sysdeal.CopiedFileSentents;
import myfirstapp.sysdeal.CopiedFilesTool;
import myfirstapp.sysdeal.CopiedSentent;
import myfirstapp.sysdeal.Tool;

import org.apache.lucene.analysis.Analyzer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.json.JSONObject;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class SingleFileCompare2 extends ViewPart {
	Button openFileButton;
	Button startButton;
	Text destFilePath;
	Label showFilePathLabel;
	Label showResultLabel;
	Table table;
	Browser browser;
	JTextPane editorPane;// 实例化一个文本编辑的控件
	DefaultPieDataset dataset = new DefaultPieDataset();
	Label resultImglabel;// 显示"涉嫌抄袭"或"通过"的图片label
	String resultString;// 文字形式检测结果

	Button addToLibrary;// 添加入库按钮
	Button showDetailButton;// 查看详细按钮
	Button exportReportButton;// 导出报告按钮

	Tool tool = new Tool();

	DecimalFormat df = new DecimalFormat("0.0");// 格式化小数，不足的补0
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	SimpleDateFormat sdfFileNameDate = new SimpleDateFormat("MM-dd HH-mm");
	/*
	 * 导出报告用到的变量
	 */
	String docFileName;// 送检文件路径，包含文件名
	JSONObject json;
	Map map;
	int paragraphCount; // 段落总数
	int wordCount; // 总字数
	int copiedSententsNum; // 重复的句子数量
	int copiedCount; // 重复的字数
	float percent; // 抄袭率
	ArrayList myListeners = new ArrayList();

	public SingleFileCompare2() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		creatSelectFileGroup(parent);
		creatMiddleGroup(parent);
		creatOptionButtons(parent);

	}

	private void creatMiddleGroup(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(2, true));

		creatShowResultGroup(composite);
		creatShowOriginalTextGroup(composite);
	}

	/**
	 * 文件选择区域
	 * 
	 * @param parent
	 */
	public void creatSelectFileGroup(final Composite parent) {

		Group outerGroup = new Group(parent, SWT.NONE);
		outerGroup.setLayout(new GridLayout(3, true));// 内层布局分为3列，宽度等分
		outerGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false)); // 本组件水平对齐，上下对齐，高度自适应，宽度填满父容器
		outerGroup.setText("单文件检查");

		destFilePath = new Text(outerGroup, SWT.BORDER | SWT.READ_ONLY);
		destFilePath
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));// 本组件水平对齐，上下对齐，高度自适应，宽度填满父容器

		openFileButton = new Button(outerGroup, SWT.NONE);
		openFileButton.setText(" 选择文件...");
		openFileButton.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, // 本组件靠左对齐，上下对齐，高度填满父容器，宽度填满父容器
				true));
		openFileButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				FileDialog fd = new FileDialog(parent.getShell(), SWT.OPEN
						| SWT.MULTI);
				fd.setText("Open");
				fd.setFilterPath("C:/");
				fd.setFilterExtensions(App.vaildFileType);
				String path = fd.open();
				// 显示文件名
				if (path != null) {
					docFileName = path;
					destFilePath.setText(path);
					startButton.setEnabled(true);
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		startButton = new Button(outerGroup, SWT.NONE);
		startButton
				.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, true)); // 本组件靠右对齐，上下对齐，高度填满父容器，宽度填满父容器
		startButton.setText("   开始    ");
		startButton.setEnabled(false);
		startButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Image image = new Image(null, this.getClass()
						.getResourceAsStream("/img/comparing.png"));
				resultImglabel.setImage(image);
				beginCompare(parent);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

	}

	/**
	 * 查询结果显示区域
	 * 
	 * @param parent
	 */
	private void creatShowResultGroup(Composite parent) {
		Group outerGroup2 = new Group(parent, SWT.NONE);
		outerGroup2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outerGroup2.setLayout(new GridLayout(2, true));// 内层布局分为2列，宽度等分
		outerGroup2.setText("检查结果");

		// 检测结果图表显示
		JFreeChart chart = ChartFactory.createPieChart(null, // chart_title
				dataset, // data
				true, // include legend
				true,//
				false//
				);
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setSectionPaint("自写率", Color.green);
		plot.setSectionPaint("重复率", Color.RED);
		plot.setSectionOutlinesVisible(false);
		plot.setNoDataMessage("No data available");
		plot.setCircular(false);
		plot.setLabelGap(0.02);

		ChartComposite frame1 = new ChartComposite(outerGroup2, SWT.RIGHT,
				chart, true);
		frame1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		frame1.setBounds(0, 0, 600, 400);

		// 显示是否通过图片
		resultImglabel = new Label(outerGroup2, SWT.NONE);
		// resultImglabel.setText("检查结果显示区");
		Image image = new Image(null, this.getClass().getResourceAsStream(
				"/img/default.png"));

		resultImglabel.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true,
				false));
		resultImglabel.setImage(image);

		// 被检测文件
		showFilePathLabel = new Label(outerGroup2, SWT.LEFT);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		showFilePathLabel.setLayoutData(gridData);
		// 检测结果文字显示
		showResultLabel = new Label(outerGroup2, SWT.LEFT);
		showResultLabel.setLayoutData(gridData);

	}

	/**
	 * 原文相似句子高亮显示区
	 * 
	 * @param parent
	 */
	private void creatShowOriginalTextGroup(Composite parent) {
		Group outerGroup3 = new Group(parent, SWT.NONE);
		outerGroup3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outerGroup3.setLayout(new GridLayout(1, true));
		outerGroup3.setText("原文查看");
		Composite comp = new Composite(outerGroup3, SWT.EMBEDDED);// SWT.EMBEDDED必须
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final java.awt.Frame frame = SWT_AWT.new_Frame(comp);// 只能放Frame不能放JFrame
		editorPane = new JTextPane();// 实例化一个文本编辑的控件
		JScrollPane scl = new JScrollPane(editorPane);
		// editorPane.setDocument(doc);
		frame.add(scl);

	}

	/**
	 * 其他操作按钮区（导出报告，添加入库）
	 * 
	 * @param parent
	 */
	private void creatOptionButtons(final Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		composite.setLayout(new GridLayout(3, true));

		addToLibrary = new Button(composite, SWT.NONE);
		addToLibrary.setText("添加入库");
		addToLibrary.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, // 本组件靠左对齐，上下对齐，高度填满父容器，宽度填满父容器
				true));
		addToLibrary.setEnabled(false);
		addToLibrary.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				final String docsPath = destFilePath.getText();

				ProgressMonitorDialog pmd = new ProgressMonitorDialog(parent
						.getShell());
				IRunnableWithProgress rwp = new IRunnableWithProgress() {

					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						// totalWork为IProgressMonitor.UNKNOWN时表示不知道任务的总量
						// 将在进度条上显示一个来回移动的进度条
						monitor.beginTask("添加入库", IProgressMonitor.UNKNOWN);
						monitor.subTask("正在完成添加，请等待……");

						final Analyzer analyzer = App.App_lucene_analyzer;
						// 这段代码实际上会被放在UI线程中执行
						IndexFile index = new IndexFile(
								docsPath, null);
						index.index(App.INDEX_MODE_APPEND);
						monitor.done();

					}
				};
				try {
					pmd.run(true, false, rwp);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				MessageBox messageBox = new MessageBox(parent.getShell(),
						SWT.ICON_INFORMATION | SWT.OK);
				messageBox.setText("提示");
				messageBox.setMessage("加入索引库成功！");
				messageBox.open();

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		showDetailButton = new Button(composite, SWT.NONE);
		showDetailButton.setText("查看详细信息");
		showDetailButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER,
				true, // 本组件靠左对齐，上下对齐，高度填满父容器，宽度填满父容器
				true));
		showDetailButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Plugin.getInstance().initAndInvoke(myListeners, 1 + "");

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		exportReportButton = new Button(composite, SWT.NONE);
		exportReportButton.setText("导出报告");
		exportReportButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER,
				true, // 本组件靠左对齐，上下对齐，高度填满父容器，宽度填满父容器
				true));
		exportReportButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(parent
						.getShell());
				IRunnableWithProgress rwp = new IRunnableWithProgress() {

					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						// totalWork为IProgressMonitor.UNKNOWN时表示不知道任务的总量
						// 将在进度条上显示一个来回移动的进度条
						monitor.beginTask("任务1" + "", IProgressMonitor.UNKNOWN);
						try {
							saveReport(json);
							System.out.println("单文件导出完毕");
						} catch (DocumentException | IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						monitor.done();

					}
				};
				try {
					pmd.run(true, false, rwp);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	/**
	 * 执行单文件比对
	 * 
	 * @param parent
	 */
	public void beginCompare(final Composite parent) {
		new Thread() {
			public void run() {
				parent.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						showFilePathLabel.setText("被检测文件："
								+ destFilePath.getText());
						// table.removeAll();
						// table.update();
						// lucene进行查找，这段代码实际上会被放在UI线程中执行
						// new SearchFiles(t).search(destFilePath
						// .getText());
						// 解析文件

						// json = method.paseDocument(editorPane,
						// destFilePath.getText());
						map = tool.paseDocument(destFilePath.getText(),
								editorPane);
						paragraphCount = (int) map.get("paragraphCount");
						wordCount = (int) map.get("countNum");
						copiedSententsNum = (int) map.get("copiedSententsNum");
						copiedCount = (int) map.get("copiedWordNum");
						percent = (float) copiedCount / wordCount * 100;

						showResultLabel.setText("段落总数：" + paragraphCount
								+ "  总字数：" + wordCount + "  重复字数："
								+ copiedCount + "  重复率：" + df.format(percent)
								+ "%");

						CopiedFilesTool.getInstance().clearAllFile();
						CopiedFileSentents copiedFileSentents = (CopiedFileSentents) map
								.get("copiedFileSentents");
						CopiedFilesTool.getInstance().addFile(
								copiedFileSentents);

						// 饼图数据
						dataset.setValue("重复率", copiedCount);
						dataset.setValue("自写率", wordCount - copiedCount);
						// 查询 结果图片
						Image image;
						if (percent < 40) {
							image = new Image(null, this.getClass()
									.getResourceAsStream("/img/pass.png"));
							addToLibrary.setEnabled(true);
							resultString = App.RESULTSTRING_PASS;
						} else {
							image = new Image(null, this.getClass()
									.getResourceAsStream("/img/copied.png"));
							addToLibrary.setEnabled(false);
							resultString = App.RESULTSTRING_FAIL;
						}

						resultImglabel.setImage(image);

					}

				});
			}
		}.start();
	}

	@SuppressWarnings("unchecked")
	public void saveReport(JSONObject json) throws DocumentException,
			IOException {
		com.itextpdf.text.Document document = new com.itextpdf.text.Document(
				PageSize.A4, 50, 50, 50, 50);
		PdfWriter writer;

		try {
			writer = PdfWriter.getInstance(document, new FileOutputStream(
					"D:\\单文件检测报告_" + sdfFileNameDate.format(new Date())
							+ ".pdf"));
		} catch (FileNotFoundException | DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		document.open();
		// 标题字体
		Font titleFont = new Font(BaseFont.createFont(
				"C:/Windows/Fonts/simkai.ttf", BaseFont.IDENTITY_H,
				BaseFont.NOT_EMBEDDED), 20);
		// 基本字体
		Font baseFont = new Font(BaseFont.createFont(
				"C:/Windows/Fonts/simfang.ttf", BaseFont.IDENTITY_H,
				BaseFont.NOT_EMBEDDED), 13);
		// 红色字体
		Font redFont = new Font();
		redFont = new Font(BaseFont.createFont("C:/Windows/Fonts/simfang.ttf",
				BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED), 12);
		redFont.setColor(BaseColor.RED);

		// 报告标题
		System.out.println(Platform.getInstanceLocation().getURL().getPath());
		// com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance
		// (System.getProperty("user.dir")+"\\icons\\electricLOGO.png");
		Paragraph pdfTitle = new Paragraph(App.PDFREPORT_TITLE, titleFont);
		// pdfTitle.add(image);
		pdfTitle.setAlignment(1);
		pdfTitle.setSpacingAfter(30);
		// 报告生成时间

		Paragraph creatDate = new Paragraph("报告生成时间：" + sdf.format(new Date()),
				baseFont);
		creatDate.setSpacingAfter(20);
		PdfPTable table = new PdfPTable(4);
		table.getDefaultCell().setMinimumHeight(40);
		table.setWidthPercentage(100);
		table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

		table.addCell(new Phrase("文件名", baseFont));
		PdfPCell cell;
		cell = new PdfPCell(new Phrase(this.docFileName, baseFont));
		cell.setColspan(3);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);
		table.addCell(new Phrase("段落", baseFont));
		table.addCell(paragraphCount + "");
		table.addCell(new Phrase("总字数", baseFont));
		table.addCell(wordCount + "");
		table.addCell(new Phrase("重复语句", baseFont));
		table.addCell(copiedSententsNum + "");
		table.addCell(new Phrase("重复语句字数", baseFont));
		table.addCell(copiedCount + "");
		table.addCell(new Phrase("重复率", baseFont));
		table.addCell(df.format(percent) + "%");
		table.addCell(new Phrase("检测结果", baseFont));
		if (resultString.equals(App.RESULTSTRING_PASS)) {
			table.addCell(new Phrase(resultString, baseFont));
		} else {
			table.addCell(new Phrase(resultString, redFont));
		}

		try {
			document.add(pdfTitle);
			document.add(creatDate);

			document.add(table);

		} catch (DocumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Paragraph paragraph1 = new Paragraph(
				new Paragraph("以下为详细信息：", baseFont));
		paragraph1.setSpacingBefore(40);
		paragraph1.setSpacingAfter(20);
		document.add(paragraph1);

		List<CopiedFileSentents> list = CopiedFilesTool.getInstance()
				.getFilesList();
		CopiedFileSentents c = list.get(0);
		List<CopiedSentent> l2 = c.getCopiedStentsList();
		float[] widths2 = { 0.1f, 0.5f, 0.4f };
		PdfPTable singleTable = new PdfPTable(widths2);
		singleTable.setWidthPercentage(100);
		singleTable.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
		singleTable.getDefaultCell().setHorizontalAlignment(
				Element.ALIGN_CENTER);

		singleTable.addCell(new Phrase("序号", baseFont));
		singleTable.addCell(new Phrase("涉嫌抄袭句子", baseFont));
		singleTable.addCell(new Phrase("出处", baseFont));
		int i = 1;
		for (final CopiedSentent sentent : l2) {

			singleTable.addCell(new Phrase(i + "", baseFont));
			singleTable.addCell(new Phrase(sentent.getOriginalSentents(),
					baseFont));
			singleTable.addCell(new Phrase(sentent.getFileName(), baseFont));
			i++;
		}
		try {
			document.add(singleTable);
		} catch (DocumentException e1) {
			e1.printStackTrace();
		}
		document.close();

	}
}