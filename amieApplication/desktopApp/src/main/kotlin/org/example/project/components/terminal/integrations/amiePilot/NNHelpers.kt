package org.example.project.components.terminal.integrations.amiePilot

import org.tensorflow.Graph
import org.tensorflow.Operand
import org.tensorflow.SavedModelBundle
import org.tensorflow.Session
import org.tensorflow.Signature
import org.tensorflow.ndarray.Shape
import org.tensorflow.op.Ops
import org.tensorflow.op.core.Assign
import org.tensorflow.op.core.Placeholder
import org.tensorflow.op.core.Variable
import org.tensorflow.types.TFloat32

class BertDownstreamClassifier(
    private val inputDim: Long = 768L,
    private val hiddenDim: Long = 128L,
    private val numClasses: Long = 9L
) {


    fun trainAndSaveModel(exportDirPath: String) {
        Graph().use { graph ->

            val tf = Ops.create(graph)

            // 1. Placeholders
            val inputs: Placeholder<TFloat32> = tf.placeholder(
                TFloat32::class.java,
                Placeholder.shape(Shape.of(-1, inputDim))
            )
            val labels: Placeholder<TFloat32> = tf.placeholder(
                TFloat32::class.java,
                Placeholder.shape(Shape.of(-1, numClasses))
            )

            // 2. Uninitialized Variables
            val w1: Variable<TFloat32> = tf.variable(Shape.of(inputDim, numClasses), TFloat32::class.java)
            val b1: Variable<TFloat32> = tf.variable(Shape.of(numClasses), TFloat32::class.java)

            // 3. Initial Value Generators & Assign Ops (REPLACES .initializer())
            val w1InitVal = tf.random.truncatedNormal(tf.constant(longArrayOf(inputDim, numClasses)), TFloat32::class.java)
            val b1InitVal = tf.zeros(tf.constant(longArrayOf(numClasses)), TFloat32::class.java)

            val initW1: Assign<TFloat32> = tf.assign(w1, w1InitVal)
            val initB1: Assign<TFloat32> = tf.assign(b1, b1InitVal)

            // 4. Forward Pass
            val matMul = tf.linalg.matMul(inputs, w1)
            val logits = tf.math.add(matMul, b1)
            val predictions = tf.nn.softmax(logits)

            // 5. Loss Operation
            val lossTensor = tf.nn.softmaxCrossEntropyWithLogits(logits, labels).loss()
            val loss = tf.math.mean(lossTensor, tf.constant(0))

            // 6. Execution Session
            Session(graph).use { session ->
                // Execute initializers (Run assign ops)
                session.runner()
                    .addTarget(initW1)
                    .addTarget(initB1)
                    .run()

                // Run forward pass / loss computation
                val lossResult = session.runner()
                    .fetch(loss)
                    .run()

                if (true) {
                    val resultTensor = lossResult.get(0)
                    println("Initial Training Loss: $resultTensor")
                    resultTensor.close()
                }
                /*
                val signature: Signature = Signature.builder()
                    .input("embedding_inputs", inputs.asOutput().asSymbol())
                    .output("class_probabilities", predictions.asOutput().asSymbol())
                    .build()
                 */

                SavedModelBundle.exporter(exportDirPath)
                    .withSession(session)
                    .withTags("serve")
                    .export()


                println("Model successfully exported to: $exportDirPath")
            }
        }
    }
}

fun createModel(inputDim: Long, hiddenDim: Long, numClasses: Long, exportDirPath: String) {
    BertDownstreamClassifier(inputDim, hiddenDim, numClasses).trainAndSaveModel(exportDirPath)
}