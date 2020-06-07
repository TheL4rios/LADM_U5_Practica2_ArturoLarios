package mx.edu.ittepic.ladm_u5_practica2_arturolarios

import android.graphics.Point
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment

    private var isTracking = false
    private var isHitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = sceneform as ArFragment
        arFragment.arSceneView.scene.addOnUpdateListener { frame ->
            arFragment.onUpdate(frame)
            onUpdate()
        }

        floatingActionButton.setOnClickListener {
            addObject(Uri.parse("model.sfb"))
        }

        showButton(false)
    }

    private fun addObject(model: Uri?)
    {
        val frame = arFragment.arSceneView.arFrame
        val point = getScreenCenter()

        frame?.let {
            val hits = frame.hitTest(point.x.toFloat(), point.y.toFloat())

            hits.forEach { hit ->
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose))
                {
                    placeObject(arFragment, hit.createAnchor(), model)
                }
            }
        }
    }

    private fun placeObject(arFragment: ArFragment, createAnchor: Anchor?, model: Uri?)
    {
        ModelRenderable.builder()
            .setSource(arFragment.context, model)
            .build()
            .thenAccept {
                addNodeToScene(arFragment, createAnchor, it)
            }
            .exceptionally {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                return@exceptionally null
            }
    }

    private fun addNodeToScene(arFragment: ArFragment, createAnchor: Anchor?, model: ModelRenderable?)
    {
        val anchorNode = AnchorNode(createAnchor)
        val transformable = TransformableNode(arFragment.transformationSystem)
        transformable.renderable = model
        transformable.setParent(anchorNode)
        arFragment.arSceneView.scene.addChild(anchorNode)
        transformable.select()
    }

    private fun showButton(toShow: Boolean)
    {
        if (toShow)
        {
            floatingActionButton.isEnabled = true
            floatingActionButton.visibility = View.VISIBLE
            return
        }

        floatingActionButton.isEnabled = false
        floatingActionButton.visibility = View.INVISIBLE
    }

    private fun onUpdate()
    {
        updateTracking()

        if (isTracking)
        {
            val hitTestChanged = updateHit()
            if (hitTestChanged)
            {
                showButton(isHitting)
            }
        }
    }

    private fun updateTracking()
    {
        val frame = arFragment.arSceneView.arFrame
        isTracking = frame?.camera?.trackingState == TrackingState.TRACKING
    }

    private fun getScreenCenter(): Point
    {
        val view = findViewById<View>(android.R.id.content)
        return Point(view.width / 2, view.height / 2)
    }

    private fun updateHit(): Boolean
    {
        val frame = arFragment.arSceneView.arFrame
        val point = getScreenCenter()
        val hits: List<HitResult>
        val wasHitting = isHitting
        isHitting = false

        frame?.let {
            hits = frame.hitTest(point.x.toFloat(), point.y.toFloat())

            hits.forEach { hit ->
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose))
                {
                    isHitting = true
                }
            }
        }

        return wasHitting != isHitting
    }
}
