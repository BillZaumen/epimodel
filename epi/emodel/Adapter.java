package emodel;
import java.awt.*;
import java.awt.geom.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.bzdev.gio.OutputStreamGraphics;
import org.bzdev.geom.*;
import org.bzdev.graphs.*;
import org.bzdev.lang.MathOps;
import org.bzdev.math.RungeKuttaMV;
import org.bzdev.math.Functions;
import org.bzdev.util.ExpressionParser;
import org.bzdev.util.ExpressionParser.ESPFunction;
import org.bzdev.net.*;
import org.bzdev.net.ServletAdapter.ServletException;

public class Adapter implements ServletAdapter {

    ExpressionParser ep = null;

    public void init(Map<String,String> parameters) {
	try {
	    ep = new
		ExpressionParser(Math.class, MathOps.class, Functions.class);
	    ep.setScriptingMode();
	} catch (IllegalAccessException e) {
	    // should not happen.
	    e.printStackTrace(System.err);
	}
    }

    public void doGet(HttpServerRequest req, HttpServerResponse res)
	throws IOException, ServletException
    {
	res.setHeader("Content-type", "image/svg+xml");
	// SVG can be readily compressed, so this will save
	// bandwidth and reduce the number of context switches
	// between user and kernel space.
	res.setHeader("Content-encoding", "gzip");
	res.sendResponseHeaders(200, 0);
	OutputStream os = new GZIPOutputStream(res.getOutputStream());
	OutputStreamGraphics osg =
	    OutputStreamGraphics.newInstance(os, 600, 800, "svg");
	Graph graph = new Graph (osg);
	graph.setOffsets(50, 75, 75, 30);
	String query = req.getQueryString();
	String script = null;
	try {
	    if (query == null) throw new Exception("no query");
	    Map<String,String> qmap = WebDecoder.formDecode(query);
	    int xmax = Integer.parseInt(qmap.get("XMax"));
	    int xmaxm1 = xmax - 1;
	    double ymax = Double.parseDouble(qmap.get("YMax"));
	    double N = Double.parseDouble(qmap.get("N"));
	    double ne0 = (qmap.containsKey("NE0"))?
		Double.parseDouble(qmap.get("NE0")):
		N * Double.parseDouble(qmap.get("NE0Percent"))/100.0;
	    double ni0 = (qmap.containsKey("NI0"))?
		Double.parseDouble(qmap.get("NI0")):
		N * Double.parseDouble(qmap.get("NI0Percent"))/100.0;
	    double n0 = (qmap.containsKey("N0"))?
		Double.parseDouble(qmap.get("N0")):
		ne0 + ni0 + N*Double.parseDouble(qmap.get("NR0Percent"))/100.0;
	    // double R0 = Double.parseDouble(qmap.get("R0"));
	    script = "function (t) { " + qmap.get("R0") + "}";
	    ExpressionParser.ESPFunction R0f = (ExpressionParser.ESPFunction)
		ep.parse(script);
	    double tauE = Double.parseDouble(qmap.get("TAU_E"));
	    double tauI = Double.parseDouble(qmap.get("TAU_I"));
	    double nV0 = (qmap.containsKey("NV0"))?
		Double.parseDouble(qmap.get("NV0")):
		N * Double.parseDouble(qmap.get("NV0Percent"))/ 100.0;
	    double vMax = (qmap.containsKey("V_MAX"))?
		Double.parseDouble(qmap.get("V_MAX")):
		N * Double.parseDouble(qmap.get("V_MAXPercent"))/100.0;
	    double tauV = Double.parseDouble(qmap.get("TAU_V"));
	    double nt = ne0 + ni0;
	    if (n0 < nt) n0 = nt;
	    
	    RungeKuttaMV rk = new RungeKuttaMV(5) {
		    protected void applyFunction(double t, double[] y,
						 double[] yprime)
		    {
			double nE = y[0];
			double nI = y[1];
			double n = y[2];
			double nV = y[3];
			double nVA = y[4];
			if (nV > N) nV = N;
			if (n > N) n = N;
			double R0 = ((Number)(R0f.invoke(t))).doubleValue();
			double scaledR0 = R0*(1-(n+nV)/N);
			// to avoid double counting: vaccinating those who
			// are exposed, infected, or recovered does not
			// alter the number who can be infected.
			double nfactor = (N - n)/N;
			// for roundoff errors.
			if (scaledR0 < 0.0) scaledR0 = 0.0;
			yprime[0] = -nE/tauE + (scaledR0/tauI)*nI;
			yprime[1] = nE/tauE - nI/tauI;
			yprime[2] = (scaledR0/tauI)*nI;
			yprime[3] = (nV + n >= N || nVA >= vMax)? 0.0:
			    nfactor*(vMax/tauV);
			yprime[4] = (nVA >= vMax)? 0.0: vMax/tauV;
		    }
		};
	    double[] initialValues = {ne0, ni0, n0, ((N-n0)/N)*nV0, nV0};
	    rk.setInitialValues(0.0, initialValues);
	    BasicSplinePathBuilder spb1 = new BasicSplinePathBuilder();
	    BasicSplinePathBuilder spb2 = new BasicSplinePathBuilder();
	    spb1.append(new SplinePathBuilder.CPoint
			(SplinePathBuilder.CPointType.MOVE_TO, 0.0, ni0));
	    spb2.append(new SplinePathBuilder.CPoint
			(SplinePathBuilder.CPointType.MOVE_TO,
			 0.0, n0*100.0/N));
	    for (int i = 0; i < xmax; i++) {
		double tincr = 1.0;
		int m = 50;
		rk.update(tincr, m);
		SplinePathBuilder.CPointType type = (i < xmaxm1)?
		    SplinePathBuilder.CPointType.SPLINE:
		    SplinePathBuilder.CPointType.SEG_END;
		spb1.append(new SplinePathBuilder.CPoint(type,
							 (double)(i+1),
							 rk.getValue(1)));
		double val = rk.getValue(2)*100.0/N;
		spb2.append(new SplinePathBuilder.CPoint
			    (type, (double)(i+1), val));
	    }
	    BasicSplinePath2D path1 = spb1.getPath();
	    BasicSplinePath2D path2 = spb2.getPath();

	    graph.setRanges(0.0, xmax, 0.0, ymax);
	    
	    Graph graph2 = new Graph(graph, true);

	    graph2.setRanges(0.0, xmax, 0.0, 100.0);


	    AxisBuilder xab = new AxisBuilder.Linear(graph,
						     0.0, 0.0, xmax, true,
						     "Time (days)");
	    AxisBuilder yab = new AxisBuilder.Linear
		(graph, 0.0, 0.0, ymax, false, "Number that are Infectious");
	    AxisBuilder.Linear yab2 =
		new AxisBuilder.Linear(graph2, xmax, 0.0, 100.0,
				       false, true,
				       "Percent infected or not Susceptible");
	    yab2.setMaximumExponent(2);
	    yab2.addTickSpec(0, 0, true, "%3.0f");
	    yab2.addTickSpec(2, 1, false, null);
	    xab.setWidth(2.0);
	    yab.setWidth(2.0);
	    yab2.setWidth(2.0);
	    // label positions need some tuning as this is an
	    // unusual case.
	    xab.setLabelOffset(15.0);
	    yab.setLabelOffset(10.0);
	    yab2.setLabelOffset(-30.0);
	    yab2.setColor(Color.BLUE);
	    // now draw the axes
	    graph.draw(xab.createAxis());
	    graph.draw(yab.createAxis());
	    graph2.draw(yab2.createAxis());
	    
	    Graphics2D g2d = graph.createGraphics();
	    g2d.setColor(Color.BLACK);
	    
	    g2d.setStroke(new BasicStroke(2.0F));
	    Point2D pt1 = graph.coordTransform(0.0, ymax);
	    Point2D pt2 = new Point2D.Double(pt1.getX()+20.0, pt1.getY());
	    Line2D line = new Line2D.Double(pt1, pt2);
	    g2d.draw(line);
	    pt1 = new Point2D.Double(pt2.getX() + 10.0, pt1.getY());
	    pt2 = graph.invCoordTransform(pt1);
	    graph.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
	    graph.setFontBaseline(Graph.BLineP.CENTER);
	    graph.setFontJustification(Graph.Just.LEFT);
	    graph.drawString(""+ ymax, pt2.getX(), ymax);

	    g2d.setStroke(new BasicStroke(1.5F));
	    Graphics2D g2d2 = graph2.createGraphics();
	    g2d2.setColor(Color.BLUE);
	    g2d2.setStroke(new BasicStroke(1.5F));

	    graph.draw(g2d, path1);
	    graph2.draw(g2d2, path2);

	} catch (Exception e) {
	    graph.setRanges(0.0, 100.0, 0.0, 100.0);
	    Graphics2D g2d = graph.createGraphics();
	    graph.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
	    String msg = e.getMessage();
	    if (msg == null) {
		msg = e.getClass().toString();
	    } else {
		msg = e.getClass().getSimpleName() + ": " + msg;
	    }
	    graph.drawString(msg, 0.0, 75.0);

	    graph.drawString("R0: " + script, 0.0, 55.0);
	}
	graph.write();
	osg.flush();
	osg.close();
    }
}
