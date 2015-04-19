package libgdx.thrust.copter;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ThrustCopter extends ApplicationAdapter
{
    private final Vector2 DAMPING = new Vector2(0.99f, 0.99f);

    private static final float TAP_DRAW_TIME_MAX = 1.0f;

    private static final int TOUCH_IMPULSE = 500;

    private FPSLogger fpsLogger;

    private SpriteBatch batch;

    private TextureAtlas.AtlasRegion background;

    private OrthographicCamera camera;

    private TextureRegion terrainBelow;
    private TextureRegion terrainAbove;
    private TextureRegion tapIndicator;
    private TextureRegion tap1;
    private TextureRegion gameOver;

    private Animation plane;

    private float terrainOffset;
    private float planeAnimTime;
    private float tapDrawTime;

    // Скорость
    private Vector2 planeVelocity = new Vector2();
    private Vector2 scrollVelocity = new Vector2();
    private Vector2 planePosition = new Vector2();
    private Vector2 planeDefaultPosition = new Vector2();
    private Vector2 gravity = new Vector2();

    private Vector3 touchPosition = new Vector3();

    private Viewport viewport;

    private GameState gameState = GameState.INIT;

    static enum GameState
    {
        INIT, ACTION, GAME_OVER
    }

    @Override
    public void create()
    {

        fpsLogger = new FPSLogger();

        createCamera();

        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("ThrustCopter.pack"));

        createTextRegions(atlas);

        Sprite backgroundSprite = new Sprite(background);
        backgroundSprite.setPosition(0, 0);

        // Переворачиваем
        terrainAbove.flip(true, true);

        batch = new SpriteBatch();

        plane = new Animation(0.01f,
                new TextureRegion(atlas.findRegion("planeRed1")),
                new TextureRegion(atlas.findRegion("planeRed2")),
                new TextureRegion(atlas.findRegion("planeRed3")),
                new TextureRegion(atlas.findRegion("planeRed2")));
        plane.setPlayMode(Animation.PlayMode.LOOP);

        resetScene();
    }

    @Override
    public void resize(int width, int height)
    {
        viewport.update(width, height);
    }

    @Override
    public void render()
    {
        Gdx.gl.glClearColor(1, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        fpsLogger.log();

        updateScene();
        drawScene();
    }

    private void drawScene()
    {
        camera.update();

        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        drawBackground();

        drawTerrainBelow();
        drawTerrainAbove();

        batch.draw(plane.getKeyFrame(planeAnimTime), planePosition.x,
                planePosition.y);

        if (tapDrawTime > 0)
        {
            batch.draw(tapIndicator, touchPosition.x - 29.5f,
                    touchPosition.y - 29.5f);
            //29.5 is half width/height of the image
        }

        if (gameState == GameState.INIT)
        {
            batch.draw(tap1, planePosition.x, planePosition.y - 80);
        }

        if (gameState == GameState.GAME_OVER)
        {
            batch.draw(gameOver, 400 - 206, 240 - 80);
        }

        batch.end();
    }

    private void resetScene()
    {
        terrainOffset = 0;
        planeAnimTime = 0;

        scrollVelocity.set(4, 0);
        planeVelocity.set(400, 0);
        gravity.set(0, -4);

        planeDefaultPosition.set(400 - 88 / 2, 240 - 73 / 2);
        planePosition.set(planeDefaultPosition.x, planeDefaultPosition.y);
    }

    private void updateScene()
    {
        if (Gdx.input.justTouched())
        {
            if (gameState == GameState.INIT)
            {
                gameState = GameState.ACTION;
                return;
            }

            if (gameState == GameState.GAME_OVER)
            {
                gameState = GameState.INIT;
                resetScene();
                return;
            }

            Vector2 tmpVector = new Vector2();

            touchPosition.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPosition);

            tmpVector.set(planePosition.x, planePosition.y);
            tmpVector.sub(touchPosition.x, touchPosition.y).nor();

            planeVelocity.mulAdd(tmpVector,
                    TOUCH_IMPULSE - MathUtils.clamp(Vector2.dst(touchPosition.x,
                                    touchPosition.y, planePosition.x, planePosition.y), 0,
                            TOUCH_IMPULSE));

            tapDrawTime = TAP_DRAW_TIME_MAX;
        }

        if (gameState == GameState.INIT || gameState ==
                GameState.GAME_OVER)
        {
            return;
        }

        float deltaTime = Gdx.graphics.getDeltaTime();

        planeAnimTime += deltaTime;

        planeVelocity.scl(DAMPING);
        planeVelocity.add(gravity);
        planeVelocity.add(scrollVelocity);

        planePosition.mulAdd(planeVelocity, deltaTime);

        terrainOffset -= planePosition.x - planeDefaultPosition.x;

        planePosition.x = planeDefaultPosition.x;

        if (terrainOffset * -1 > terrainBelow.getRegionWidth())
        {
            terrainOffset = 0;
        }

        if (terrainOffset > 0)
        {
            terrainOffset = -terrainBelow.getRegionWidth();
        }

        if (planePosition.y < terrainBelow.getRegionHeight() - 35 || planePosition.y + 73 > 480 -
                terrainBelow.getRegionHeight() + 35)
        {
            if (gameState != GameState.GAME_OVER)
            {
                gameState = GameState.GAME_OVER;
            }
        }

        tapDrawTime -= deltaTime;
    }

    private void drawTerrainAbove()
    {
        batch.draw(terrainAbove, terrainOffset, 480 - terrainAbove.
                getRegionHeight());
        batch.draw(terrainAbove, terrainOffset + terrainAbove.
                getRegionWidth(), 480 - terrainAbove.getRegionHeight());
    }

    private void drawTerrainBelow()
    {
        batch.draw(terrainBelow, terrainOffset, 0);
        batch.draw(terrainBelow, terrainOffset + terrainBelow.
                getRegionWidth(), 0);
    }

    private void createTextRegions(TextureAtlas atlas)
    {
        gameOver = new TextureRegion(new Texture("gameOver.png"));

        background = atlas.findRegion("background");
        tapIndicator = atlas.findRegion("tap2");
        tap1 = atlas.findRegion("tap1");

        terrainBelow = new TextureRegion(atlas.findRegion("groundGrass"));
        terrainAbove = new TextureRegion(terrainBelow);
    }

    private void createCamera()
    {
        camera = new OrthographicCamera();
        camera.position.set(400, 240, 0);
        viewport = new FitViewport(800, 480, camera);
    }

    private void drawBackground()
    {
        // Выключаем смешивание так как рисуем
        // background-текстуру, которая не накладывается
        // на другие.
        batch.disableBlending();

        batch.draw(background, 0, 0);

        batch.enableBlending();
    }

}
