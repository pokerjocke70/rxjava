import org.apache.camel.Message
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.rx.ReactiveCamel

import java.util.concurrent.TimeUnit

import static groovyx.gpars.GParsPool.withPool

def context = new DefaultCamelContext()

context.start()

ReactiveCamel rx = new ReactiveCamel(context)

def r = rx.toObservable("seda:order?concurrentConsumers=2")
        .filter({ Message m -> m.getBody(Integer) % 2 == 0 })
        .map({ Message m -> "Mapped ${m.body}"})

rx.sendTo(r, "stream:out")

withPool(8) {
    (1..100).eachParallel {
        context.createProducerTemplate().sendBody("seda:order", it)
    }
}

TimeUnit.SECONDS.sleep(3L)

context.shutdown()
