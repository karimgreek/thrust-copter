package libgdx.thrust.copter;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ThrustCopter extends ApplicationAdapter
{
    private final Vector2 DAMPING = new Vector2(0.99f, 0.99f);
    private static final float TAP_DRAW_TIME_MAX = 1.0f;
    private static final int TOUCH_IMPULSE = 500;
    private static final int METEOR_SPEED = 60;

    private FPSLogger fpsLogger;

    private SpriteBatch batch;

    private TextureAtlas.AtlasRegion background;

    private OrthographicCamera camera;

    private TextureAtlas atlas;

    private TextureRegion terrainBelow;
    private TextureRegion terrainAbove;
    private TextureRegion tapIndicator;
    private TextureRegion tap1;
    private TextureRegion gameOver;
    private TextureRegion pillarDown;
    private TextureRegion pillarUp;
    private TextureRegion selectedMeteorTexture;

    private Array<Vector2> pillars = new Array<Vector2>();
    Array<TextureAtlas.AtlasRegion> meteorTextures = new Array<TextureAtlas.AtlasRegion>();

    private Animation plane;

    private float terrainOffset;
    private float planeAnimTime;
    private float tapDrawTime;
    private float deltaPosition;
    private float nextMeteorIn;

    private boolean meteorInScene;

    // Скорость
    private Vector2 planeVelocity = new Vector2();
    private Vector2 scrollVelocity = new Vector2();
    private Vector2 planePosition = new Vector2();
    private Vector2 lastPillarPosition = new Vector2();
    private Vector2 planeDefaultPosition = new Vector2();
    private Vector2 gravity = new Vector2();
    private Vector2 meteorPosition = new Vector2();
    private Vector2 meteorVelocity = new Vector2();

    private Vector3 touchPosition = new Vector3();

    private Viewport viewport;

    private GameState gameState = GameState.INIT;

    private Rectangle planeRect = new Rectangle();
    private Rectangle obstacleRect = new Rectangle();

    private Music music;

    private Sound tapSound;
    private Sound crashSound;
    private Sound spawnSound;

    private static enum GameState
    {
        INIT, ACTION, GAME_OVER
    }

    @Override
    public void create()
    {

        fpsLogger = new FPSLogger();

        createCamera();

        atlas = new TextureAtlas(Gdx.files.internal("ThrustCopter.pack"));

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

        music = Gdx.audio.newMusic(Gdx.files.internal("sounds/journey.mp3"));
        music.setLooping(true);
        music.play();

        tapSound = Gdx.audio.newSound(Gdx.files.internal("sounds/pop.ogg"));
        crashSound = Gdx.audio.newSound(Gdx.files.internal("sounds/crash.ogg"));
        spawnSound = Gdx.audio.newSound(Gdx.files.internal("sounds/alarm.ogg"));

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

        drawPillars();

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

        if (meteorInScene)
        {
            batch.draw(selectedMeteorTexture, meteorPosition.x,
                    meteorPosition.y);
        }

        batch.end();
    }

    private void drawPillars()
    {
        for (Vector2 vec : pillars)
        {
            if (vec.y == 1)
            {
                batch.draw(pillarUp, vec.x, 0);
            }
            else
            {
                batch.draw(pillarDown, vec.x,
                        480 - pillarDown.getRegionHeight());
            }
        }
    }

    private void resetScene()
    {
        terrainOffset = 0;
        planeAnimTime = 0;

        scrollVelocity.set(4, 0);
        planeVelocity.set(400, 0);
        gravity.set(0, -4);

        planeDefaultPosition.set(300 - 88 / 2, 240 - 73 / 2);
        planePosition.set(planeDefaultPosition.x, planeDefaultPosition.y);

        pillars.clear();
        addPillar();

        meteorInScene = false;
        nextMeteorIn = (float) Math.random() * 5;
    }

    private void updateScene()
    {
        if (Gdx.input.justTouched())
        {
            tapSound.play();

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

        deltaPosition = planePosition.x - planeDefaultPosition.x;
        terrainOffset -= deltaPosition;

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
                crashSound.play();
                gameState = GameState.GAME_OVER;
            }
        }

        pillarsLogic();

        meteorLogic(deltaTime);

        tapDrawTime -= deltaTime;
    }

    private void meteorLogic(float deltaTime)
    {
        if (meteorInScene)
        {
            meteorPosition.mulAdd(meteorVelocity, deltaTime);

            meteorPosition.x -= deltaPosition;

            if (meteorPosition.x < -10)
            {
                meteorInScene = false;
            }
        }

        nextMeteorIn -= deltaTime;

        if (nextMeteorIn <= 0)
        {
            launchMeteor();
        }

        if (meteorInScene)
        {
            obstacleRect.set(meteorPosition.x + 2, meteorPosition.y + 2,
                    selectedMeteorTexture.getRegionWidth() - 4,
                    selectedMeteorTexture.getRegionHeight() - 4);

            if (planeRect.overlaps(obstacleRect))
            {
                if (gameState != GameState.GAME_OVER)
                {
                    crashSound.play();
                    gameState = GameState.GAME_OVER;
                }
            }
        }
    }

    private void launchMeteor()
    {
        nextMeteorIn = 1.5f + (float) Math.random() * 5;

        if (meteorInScene)
        {
            return;
        }

        spawnSound.play();

        meteorInScene = true;

        int id = (int) (Math.random() * meteorTextures.size);
        selectedMeteorTexture = meteorTextures.get(id);

        meteorPosition.x = 810;
        meteorPosition.y = (float) (80 + Math.random() * 320);

        Vector2 destination = new Vector2();
        destination.x = -10;
        destination.y = (float) (80 + Math.random() * 320);
        destination.sub(meteorPosition).nor();

        meteorVelocity.mulAdd(destination, METEOR_SPEED);
    }

    private void pillarsLogic()
    {
        planeRect.set(planePosition.x + 16, planePosition.y, 50, 73);

        for (Vector2 vec : pillars)
        {
            vec.x -= deltaPosition;

            if (vec.x + pillarUp.getRegionWidth() < -10)
            {
                pillars.removeValue(vec, false);
            }

            if (vec.y == 1)
            {
                obstacleRect.set(vec.x + 10, 0, pillarUp.getRegionWidth() - 20,
                        pillarUp.getRegionHeight() - 10);
            }
            else
            {
                obstacleRect.set(vec.x + 10,
                        480 - pillarDown.getRegionHeight() + 10,
                        pillarUp.getRegionWidth() - 20, pillarUp.getRegionHeight());
            }

            if (planeRect.overlaps(obstacleRect))
            {
                if (gameState != GameState.GAME_OVER)
                {
                    crashSound.play();
                    gameState = GameState.GAME_OVER;
                }
            }
        }

        if (lastPillarPosition.x < 400)
        {
            addPillar();
        }
    }

    private void addPillar()
    {
        Vector2 pillarPosition = new Vector2();

        if (pillars.size == 0)
        {
            pillarPosition.x = (float) (800 + Math.random() * 600);
        }
        else
        {
            pillarPosition.x = lastPillarPosition.x + (float) (600 +
                    Math.random() * 600);
        }

        if (MathUtils.randomBoolean())
        {
            pillarPosition.y = 1;
        }
        else
        {
            pillarPosition.y = -1; //upside down
        }

        lastPillarPosition = pillarPosition;

        pillars.add(pillarPosition);
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

        terrainBelow = atlas.findRegion("groundGrass");
        terrainAbove = new TextureRegion(terrainBelow);

        pillarUp = atlas.findRegion("rockGrassUp");
        pillarDown = atlas.findRegion("rockGrassDown");

        initMeteorTextures(atlas);
    }

    private void initMeteorTextures(TextureAtlas atlas)
    {
        meteorTextures.add(atlas.findRegion("meteorBrown_med1"));
        meteorTextures.add(atlas.findRegion("meteorBrown_med2"));
        meteorTextures.add(atlas.findRegion("meteorBrown_small1"));
        meteorTextures.add(atlas.findRegion("meteorBrown_small2"));
        meteorTextures.add(atlas.findRegion("meteorBrown_tiny1"));
        meteorTextures.add(atlas.findRegion("meteorBrown_tiny2"));
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

    @Override
    public void dispose()
    {
        tapSound.dispose();
        crashSound.dispose();
        spawnSound.dispose();
        music.dispose();
        batch.dispose();
        pillars.clear();
        atlas.dispose();
        meteorTextures.clear();
    }

}
