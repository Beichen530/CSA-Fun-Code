/*import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.stream.IntStream;

public class LoopsPractice2 extends JPanel implements ActionListener {
    // Scaling factor (2 = renders at half screen resolution upscaled for ultra-high FPS)
    private static final int SCALE_FACTOR = 2;
    
    private int screenWidth;
    private int screenHeight;
    private int renderWidth;
    private int renderHeight;
    
    private double time = 0;
    private final Timer timer;
    private BufferedImage canvas;
    private int[] pixels;

    public LoopsPractice2(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        this.renderWidth = screenWidth / SCALE_FACTOR;
        this.renderHeight = screenHeight / SCALE_FACTOR;

        setPreferredSize(new Dimension(screenWidth, screenHeight));
        
        canvas = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

        // High refresh rate timer (~60+ FPS)
        timer = new Timer(16, this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        time += 0.045; // Speed of motion
        renderFractalPlasmaParallel();
        repaint();
    }

    private void renderFractalPlasmaParallel() {
        double aspect = (double) renderWidth / renderHeight;

        // Dynamic drifting vectors (no static center point)
        double t1 = time * 0.8;
        double t2 = time * 0.5;
        double t3 = time * 1.2;

        // Parallel processing across all CPU cores
        IntStream.range(0, renderHeight).parallel().forEach(y -> {
            int rowOffset = y * renderWidth;
            double ny = ((double) y / renderHeight) * 4.0 - 2.0;

            for (int x = 0; x < renderWidth; x++) {
                double nx = (((double) x / renderWidth) * 4.0 - 2.0) * aspect;

                // --- LAYER 1: First Distortion Pass ---
                double qx = Math.sin(nx + Math.cos(ny * 1.5 + t1) + t2);
                double qy = Math.cos(ny + Math.sin(nx * 1.5 - t2) + t1);

                // --- LAYER 2: Fractal Domain Warping (Feeds Layer 1 back into coordinates) ---
                double rx = Math.sin(nx + 3.0 * qx + Math.cos(ny + 2.0 * qy + t3));
                double ry = Math.cos(ny + 3.0 * qy + Math.sin(nx + 2.0 * qx - t3));

                // --- LAYER 3  
                double wave1 = Math.sin(rx * 6.0 + ry * 6.0 + time * 3.0);
                double wave2 = Math.cos((nx * rx - ny * ry) * 5.0 - time * 2.0);
                double wave3 = Math.sin(FastSimplexNoise.noise(rx * 1.5, ry * 1.5 + time * 0.5) * 8.0);

                double combined = wave1 + wave2 + wave3;

                // --- PSYCHEDELIC COLOR PALETTE MAPPING ---
                // Non-linear, multi-frequency RGB phase shifts
                int r = (int) (127.5 * (1.0 + Math.sin(combined * 2.0 + time * 4.0 + rx * 2.0)));
                int g = (int) (127.5 * (1.0 + Math.sin(combined * 3.0 + time * 3.0 + ry * 2.0 + 2.094))); // +120 deg
                int b = (int) (127.5 * (1.0 + Math.sin(combined * 1.5 + time * 5.0 + (rx + ry) + 4.188))); // +240 deg

                // Fast Direct Pixel Buffer Write
                pixels[rowOffset + x] = (r << 16) | (g << 8) | b;
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (canvas != null) {
            Graphics2D g2d = (Graphics2D) g;
            // Hardware accelerated scaling
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(canvas, 0, 0, screenWidth, screenHeight, null);
        }
    }

    public static void main(String[] args) {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode dm = gd.getDisplayMode();

        JFrame frame = new JFrame("LoopsPractice2 - Fully Decentralized Fractal Plasma");
        LoopsPractice2 panel = new LoopsPractice2(dm.getWidth(), dm.getHeight());

        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }
}

/*
 * Ultra-Fast Simplex Noise Helper
 
class FastSimplexNoise {
    public static double noise(double x, double y) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;

        double g1 = grad(xi, yi);
        double g2 = grad(xi + 1, yi);
        double g3 = grad(xi, yi + 1);
        double g4 = grad(xi + 1, yi + 1);

        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);

        double u = xf * xf * (3 - 2 * xf);
        double v = yf * yf * (3 - 2 * yf);

        return lerp(v, lerp(u, g1, g2), lerp(u, g3, g4));
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int x, int y) {
        int hash = (x * 374761393 + y * 668265263) ^ (x * y);
        return (hash & 1000) / 1000.0 * 2.0 - 1.0;
    }
}
*/
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.stream.IntStream;

public class LoopsPractice2 extends JPanel implements ActionListener {
    // 2 = 1/2 screen rendering upscaled via Bilinear hardware for ultra-fluid 60+ FPS
    private static final int SCALE_FACTOR = 2;
    
    private int screenWidth;
    private int screenHeight;
    private int renderWidth;
    private int renderHeight;
    
    private double time = 0;
    private final Timer timer;
    private BufferedImage canvas;
    private int[] pixels;

    public LoopsPractice2(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        this.renderWidth = screenWidth / SCALE_FACTOR;
        this.renderHeight = screenHeight / SCALE_FACTOR;

        setPreferredSize(new Dimension(screenWidth, screenHeight));
        
        canvas = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

        // 60+ FPS render loop
        timer = new Timer(15, this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        time += 0.035; // Accelerated hyper-evolution speed
        renderAbsoluteMindBenderParallel();
        repaint();
    }

    private void renderAbsoluteMindBenderParallel() {
        double aspect = (double) renderWidth / renderHeight;

        // 4D Rotation Phase Angles
        double cosT = Math.cos(time * 0.8);
        double sinT = Math.sin(time * 0.8);
        double cosT2 = Math.cos(time * 0.5);
        double sinT2 = Math.sin(time * 0.5);

        // Multithreaded parallel execution
        IntStream.range(0, renderHeight).parallel().forEach(y -> {
            int rowOffset = y * renderWidth;
            double ny = ((double) y / renderHeight) * 5.0 - 2.5;

            for (int x = 0; x < renderWidth; x++) {
                double nx = (((double) x / renderWidth) * 5.0 - 2.5) * aspect;

                // --- 4D HYPER-SPATIAL ROTATION ---
                // Rotate 2D screen coordinates into simulated 4D trigonometric coordinates (u, v, w)
                double u = nx * cosT - ny * sinT;
                double v = nx * sinT + ny * cosT;
                double w = Math.sin(u * 1.5 + v * 1.5 + time) * 1.5;

                // --- THREE-PASS DOMAIN WARPING WITH HYPERSPACE TUNNELING ---
                double qx = Math.sin(u + Math.cos(v * 2.0 + time) + w);
                double qy = Math.cos(v + Math.sin(u * 2.0 - time) + w);

                double rx = Math.sin(u * cosT2 + 3.0 * qx + Math.cos(v * sinT2 + 2.0 * qy + time * 1.2));
                double ry = Math.cos(v * cosT2 + 3.0 * qy + Math.sin(u * sinT2 + 2.0 * qx - time * 1.2));

                // Logarithmic infinite depth calculation
                double dist = Math.hypot(rx, ry);
                double tunnel = Math.log(dist + 0.001) * 2.0 - time * 2.0;

                // Wave matrix with extreme stroboscopic spatial folding
                double p1 = Math.sin(rx * 6.0 + ry * 6.0 + tunnel + time * 2.0);
                double p2 = Math.cos((rx * qx - ry * qy) * 8.0 - time * 3.0);
                double p3 = Math.sin(dist * 10.0 - tunnel * 3.0 + Math.atan2(ry, rx) * 8.0);

                double val = p1 + p2 + p3;

                // --- SPECTRAL CHROMATIC ABERRATION (RGB SPLITTING) ---
                // Separates red, green, and blue coordinates across spatial wave offsets
                double rPhase = val * 3.0 + time * 3.0 + rx * 2.0;
                double gPhase = val * 3.0 + time * 3.0 + ry * 2.0 + 2.094 + Math.sin(tunnel); // +120 deg
                double bPhase = val * 3.0 + time * 3.0 + (rx + ry) * 2.0 + 4.188 + Math.cos(tunnel); // +240 deg

                int r = (int) (127.5 * (1.0 + Math.sin(rPhase)));
                int g = (int) (127.5 * (1.0 + Math.sin(gPhase)));
                int b = (int) (127.5 * (1.0 + Math.sin(bPhase)));

                // Direct array buffer write
                pixels[rowOffset + x] = (r << 16) | (g << 8) | b;
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (canvas != null) {
            Graphics2D g2d = (Graphics2D) g;
            
            // Bilinear hardware filtering ensures zero pixelation
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            g2d.drawImage(canvas, 0, 0, screenWidth, screenHeight, null);
        }
    }

    public static void main(String[] args) {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode dm = gd.getDisplayMode();

        JFrame frame = new JFrame("LoopsPractice2 - Hyper-Dimensional Reality Warp");
        LoopsPractice2 panel = new LoopsPractice2(dm.getWidth(), dm.getHeight());

        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }
}