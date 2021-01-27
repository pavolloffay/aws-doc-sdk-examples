//snippet-sourcedescription:[<<FILENAME>> demonstrates how to send, receive and delete messages from a queue.]
//snippet-keyword:[Java]
//snippet-sourcesyntax:[java]
//snippet-keyword:[Code Sample]
//snippet-keyword:[Amazon Simple Queue Service]
//snippet-service:[sqs]
//snippet-sourcetype:[full-example]
//snippet-sourcedate:[]
//snippet-sourceauthor:[soo-aws]
/*
 * Copyright 2011-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package aws.example.sqs;
import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.Date;
import java.util.List;
import javax.jms.JMSException;

public class SendReceiveMessages
{
    private static final String QUEUE_NAME = "testQueue" + new Date().getTime();

    public static void main(String[] args) throws InterruptedException, JMSException {
        Tracer tracer = GlobalOpenTelemetry.getTracer("sqs.example");
        Span span = tracer.spanBuilder("main").startSpan();
        System.out.println(span);
        Scope scope = span.makeCurrent();

        AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
        builder.setRegion("us-east-1");
        final AmazonSQS sqs = builder.build();


        try {
            CreateQueueResult create_result = sqs.createQueue(QUEUE_NAME);
            System.out.println(create_result);
        } catch (AmazonSQSException e) {
            if (!e.getErrorCode().equals("QueueAlreadyExists")) {
                throw e;
            }
        }

        String queueUrl = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();

        SendMessageRequest send_msg_request = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody("hello world")
                .withDelaySeconds(5);
        SendMessageResult sendMessageResult = sqs.sendMessage(send_msg_request);
        System.out.println(sendMessageResult);

        // Send multiple messages to the queue
        SendMessageBatchRequest send_batch_request = new SendMessageBatchRequest()
                .withQueueUrl(queueUrl)
                .withEntries(
                        new SendMessageBatchRequestEntry(
                                "msg_1", "Hello from message 1"),
                        new SendMessageBatchRequestEntry(
                                "msg_2", "Hello from message 2")
                                .withDelaySeconds(10));
        SendMessageBatchResult sendMessageBatchResult = sqs.sendMessageBatch(send_batch_request);
        System.out.println(sendMessageBatchResult);

        // receive messages from the queue
        List<Message> messages = sqs.receiveMessage(queueUrl).getMessages();

        // delete messages from the queue
        for (Message m : messages) {
            DeleteMessageResult deleteMessageResult = sqs
                .deleteMessage(queueUrl, m.getReceiptHandle());
            System.out.println(deleteMessageResult);
        }

        // SQS with JMS
        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
            new ProviderConfiguration(),
            sqs);

// Create the connection.
        SQSConnection connection = connectionFactory.createConnection();
        // Get the wrapped client
        AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();

// Create an SQS queue named MyQueue, if it doesn't already exist
        if (!client.queueExists("MyQueue")) {
            CreateQueueResult result = client.createQueue("MyQueue");
            System.out.println(result);
        }

        scope.close();
        span.end();
        Thread.sleep(10000);
    }
}
