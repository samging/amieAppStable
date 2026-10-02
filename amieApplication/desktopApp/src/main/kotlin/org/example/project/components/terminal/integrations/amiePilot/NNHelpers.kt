package org.example.project.components.terminal.integrations.amiePilot

import org.tensorflow.Graph
import org.tensorflow.framework.optimizers.GradientDescent
import org.tensorflow.ndarray.Shape
import org.tensorflow.op.Ops
import org.tensorflow.op.core.Placeholder
import org.tensorflow.types.TFloat32
import org.tensorflow.Signature
import java.io.File
import org.tensorflow.SavedModelBundle

class BertDownstreamClassifier(
    private val inputDim: Long = 768L,   // Matches BERT contextual vector size
    private val hiddenDim: Long = 128L,  // Hidden layer neurons
    private val numClasses: Long = 9L    // e.g., 9 NER classes
) {

    fun trainModel(modelName: String) {
        Graph().use { graph ->
            val tf = Ops.create(graph)

            val inputs = tf.placeholder(TFloat32::class.java, Placeholder.shape(Shape.of(-1, inputDim)))
            val labels = tf.placeholder(TFloat32::class.java, Placeholder.shape(Shape.of(-1, numClasses)))

            val w1 = tf.variable(tf.random.truncatedNormal(tf.constant(longArrayOf(inputDim, hiddenDim)), TFloat32::class.java))
            val b1 = tf.variable(tf.zeros(tf.constant(longArrayOf(hiddenDim)), TFloat32::class.java))

            val hiddenLayer = tf.nn.relu(tf.math.add(tf.linalg.matMul(inputs, w1), b1))

            val w2 = tf.variable(tf.random.truncatedNormal(tf.constant(longArrayOf(hiddenDim, numClasses)), TFloat32::class.java))
            val b2 = tf.variable(tf.zeros(tf.constant(longArrayOf(numClasses)), TFloat32::class.java))

            val logits = tf.math.add(tf.linalg.matMul(hiddenLayer, w2), b2)
            val predictions = tf.nn.softmax(logits)

            val crossEntropy = tf.nn.softmaxCrossEntropyWithLogits(logits, labels)
            val loss = tf.math.mean(crossEntropy.loss(), tf.constant(0))

            val optimizer = GradientDescent(graph, "optimizer", 0.01f)
            val trainOp = optimizer.minimize(loss)

            println("TensorFlow Java Neural Network constructed successfully.")

                tf.session().use { session ->
                // Initialize variable weights
                session.initializer().run()

                // Optional: Run training loop here to update w1 ...

                val signature = Signature.builder()
                    .input("embedding_inputs", inputs)
                    .output("class_probabilities", predictions)
                    .build()

                SavedModelBundle.exporter(modelName)
                    .withSession(session)
                    .addTags("serve") // Tag standard for inference/serving
                    .addSignature("serving_default", signature)
                    .export()

                println("Model successfully saved to: $exportPath")
            }
        }
        }
    }
}

fun createModel(inputDim: Long, hiddenDim: Long, numClasses: Long) {
    BertDownstreamClassifier(inputDim, hiddenDim, numClasses).trainModel("bert_downstream_model")
}