import constants.Constants;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.jfree.fx.FXGraphics2D;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class GeometryDash extends Application
{
    private int playerVelocityY;
    private boolean isJumping;
    private boolean isGameOver;
    private int obstacleX;
    private BufferedImage background;
    private BufferedImage playerImage;
    private BufferedImage spikeImage;
    private AffineTransform playerAffineTransform;
    private AffineTransform spikeAffineTransform;
    private Canvas canvas;
    private double scoreTime;
    private int score;
    private int playerX;
    private int playerY;
    private int spikeY;
    private ArrayList<BufferedImage> numbers;
    private float rotate;
    private Boolean rememberRotate;
    private float lastRotate = 0;
    private Rectangle2D playerHitbox;
    private Rectangle2D spikeHitbox;

    @Override
    public void start(Stage stage)
    {
        BorderPane mainPane = new BorderPane();
        canvas = new Canvas(Constants.screenWidth, Constants.screenHeight);
        mainPane.setCenter(canvas);
        FXGraphics2D graphics2D = new FXGraphics2D(canvas.getGraphicsContext2D());
        graphics2D.setBackground(Color.WHITE);

        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(event -> {
            if (!isJumping)
            {
                lastRotate = rotate;
                isJumping = true;
                playerVelocityY = -80;
            }
        });

        new AnimationTimer()
        {
            long last = -1;

            @Override
            public void handle(long now)
            {
                if (last == -1)
                {
                    last = now;
                }
                update((now - last) / 1000000000.0);
                last = now;
                draw(graphics2D);
            }
        }.start();

        stage.setScene(new Scene(mainPane, 800, 600));
        stage.setTitle("Geometry Dash");
        stage.show();
        draw(graphics2D);
    }

    public void init() throws IOException
    {
        background = ImageIO.read(getClass().getResource("/background.png"));
        playerImage = ImageIO.read(getClass().getResource("/player.png"));
        spikeImage = ImageIO.read(getClass().getResource("/spike.png"));

        playerAffineTransform = new AffineTransform();
        playerAffineTransform.translate(playerX, playerY);
        playerAffineTransform.scale(Constants.SCALE_PLAYER, Constants.SCALE_PLAYER);

        spikeAffineTransform = new AffineTransform();
        spikeAffineTransform.translate(obstacleX, spikeY);
        spikeAffineTransform.scale(Constants.SCALE_SPIKE, Constants.SCALE_SPIKE);

        obstacleX = Constants.screenWidth + 100;

        playerX = 50;
        playerY = 550;
        spikeY = 563;

        numbers = getSubImages("numbers.png", 0, 0, 15, 24);

        rememberRotate = false;

        playerHitbox = new Rectangle2D.Double(playerX, playerY, Constants.playerWidth, Constants.playerHeight);
        spikeHitbox = new Rectangle2D.Double(obstacleX, spikeY, Constants.obstacleWidth, Constants.obstacleHeight);
    }

    public void draw(FXGraphics2D graphics2D)
    {
        graphics2D.clearRect(0, 0, Constants.screenWidth, Constants.screenHeight);

        graphics2D.setPaint(new TexturePaint(background, new Rectangle2D.Double(0,0,
                canvas.getWidth(), canvas.getHeight())));
        graphics2D.fill(new Rectangle2D.Double(0, 0, canvas.getWidth(), canvas.getHeight()));

        if (!isJumping)
        {
            drawParticals(graphics2D);
        }

        if (score != 0)
        {
            int number = score;
            ArrayList<BufferedImage> scoreImages = new ArrayList<>();

            do
            {
                int toDraw = number % 10;
                scoreImages.add(numbers.get(toDraw));
                number /= 10;
            } while(number > 0);
            drawScore(graphics2D, scoreImages, 10);
        }

        graphics2D.drawImage(playerImage, getPlayerAffineTransform(), null);
        graphics2D.drawImage(spikeImage, getSpikeAffineTransform(), null);
    }

    private void update(double deltaTime)
    {
        if (!isGameOver)
        {
            setPlayerAffineTransform(playerX, playerY + playerVelocityY, rotate);
            playerHitbox.setRect(playerX, playerY + playerVelocityY, Constants.playerWidth, Constants.playerHeight);

            obstacleX += Constants.OBSTACLE_VELOCITY_X;
            setSpikeAffineTransform(obstacleX, spikeY);
            spikeHitbox.setRect(obstacleX, spikeY, Constants.obstacleWidth, Constants.obstacleHeight);

            if (playerHitbox.intersects(spikeHitbox)) {
                isGameOver = true;
            }

            playerVelocityY += Constants.gravity;

            if (isJumping)
            {
                rememberRotate = !rememberRotate;
                rotate += 0.1f;
            }
            else
            {
                if (rememberRotate == false)
                {
                    rotate = (float) Math.PI;
                }
                else
                {
                    rotate = 0;
                }
            }

            setPlayerAffineTransform(playerX, playerY + playerVelocityY, rotate);
            scoreTime += deltaTime;

            if(scoreTime > 1)
            {
                score++;
                System.out.println(score);
                scoreTime = 0;
            }

            if (getPlayerAffineTransform().getTranslateY() + Constants.playerHeight > Constants.screenHeight)
            {
                playerY = Constants.screenHeight - Constants.playerHeight;
                playerVelocityY = 0;
                isJumping = false;
                rotate = lastRotate;
            }

            obstacleX += Constants.OBSTACLE_VELOCITY_X;
            setSpikeAffineTransform(obstacleX, spikeY);
            if (getSpikeAffineTransform().getTranslateX() + Constants.obstacleWidth < 0)
            {
                Random random = new Random();
                int max = 9;
                int min = 1;
                obstacleX = Constants.screenWidth + 100 * random.nextInt(max - min + 1) + min;
            }

            if (getSpikeAffineTransform().getTranslateX() < getPlayerAffineTransform().getTranslateX() +
                    Constants.playerWidth && getSpikeAffineTransform().getTranslateX() +
                    Constants.obstacleWidth > getPlayerAffineTransform().getTranslateX()
                    && Constants.screenHeight - Constants.obstacleHeight < getPlayerAffineTransform().getTranslateY() +
                    Constants.playerHeight && Constants.screenHeight > getPlayerAffineTransform().getTranslateY())
            {
                isGameOver = true;
            }
        }
        else
        {
            isGameOver = false;
            reset();
        }
    }

    public void drawScore(FXGraphics2D graphics2D, ArrayList<BufferedImage> images, int spacing)
    {
        int x = 100;

        for (BufferedImage image : images)
        {
            graphics2D.drawImage(image, x, 100, null);
            x -= spacing;
        }
    }

    public ArrayList<BufferedImage> getSubImages(String imageName, int x, int y, int width, int height) throws IOException
    {
        String path = System.getProperty("user.dir") + "\\resources\\" + imageName;
        File file = new File(path);
        BufferedImage image = ImageIO.read(file);

        numbers = new ArrayList<>();

        for (int i = 0; i < 11; i++)
        {
            BufferedImage subImage = image.getSubimage(x, y, width, height);
            numbers.add(subImage);
            x += width;
        }

        return numbers;
    }

    public void drawParticals(FXGraphics2D graphics2D)
    {
        // Draw particles
        graphics2D.setColor(Color.RED);
        for (int i = 0; i < 20; i++) {
            Random random = new Random();
            int value = random.nextBoolean() ? 1 : 0;

            if (value == 1)
            {
                int particleX = playerX + Constants.playerWidth - 26 - i; // particle starts in the center of player
                int particleY = playerY + Constants.playerHeight / 2 + 20; // particle starts in the center of player
                graphics2D.fillRect(particleX, particleY, 5, 5); // draw a square particle
            }
        }
    }


    private AffineTransform getSpikeAffineTransform()
    {
        return spikeAffineTransform;
    }

    public AffineTransform getPlayerAffineTransform()
    {
        return playerAffineTransform;
    }

    public void setPlayerAffineTransform(int x, int y)
    {
        playerAffineTransform.setToIdentity();
        playerAffineTransform.translate(x, y);
        playerAffineTransform.scale(Constants.SCALE_PLAYER, Constants.SCALE_PLAYER);
    }

    public void setPlayerAffineTransform(int x, int y, double rotate)
    {
        playerAffineTransform.setToIdentity();
        playerAffineTransform.translate(x, y);
        playerAffineTransform.scale(Constants.SCALE_PLAYER, Constants.SCALE_PLAYER);
        playerAffineTransform.rotate(rotate, playerImage.getWidth() / 2, playerImage.getHeight() / 2);
    }

    public void setSpikeAffineTransform(int x, int y)
    {
        spikeAffineTransform.setToIdentity();
        spikeAffineTransform.translate(x, y);
        spikeAffineTransform.scale(Constants.SCALE_SPIKE, Constants.SCALE_SPIKE);
    }

    private void reset()
    {
        System.out.println("reset");
        playerX = 50;
        playerY = 550;
        playerVelocityY = 0;
        isJumping = false;
        isGameOver = false;
        obstacleX = Constants.screenWidth + 100;
        scoreTime = 0;
        score = 0;
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
