/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mongodb;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Sorts.descending;

/**
 * Adopted form https://mongodb.github.io/mongo-java-driver/3.0/getting-started
 */
public class QuickTour {

    public static void run(MongoClient mongoClient) {

        MongoDatabase database = mongoClient.getDatabase("mydb");
        MongoCollection<Document> collection = database.getCollection("test");
        collection.drop();

        // make a document and insert it
        Document doc = new Document("name", "MongoDB").append("type", "database").append("count", 1).append("info",
                new Document("x", 203).append("y", 102));

        collection.insertOne(doc);

        // get it (since it's the only one in there since we dropped the rest earlier on)
        Document myDoc = collection.find().first();
        System.out.println(myDoc.toJson());

        // now, lets add lots of little documents to the collection so we can explore queries and cursors
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }
        collection.insertMany(documents);
        System.out.println("total # of documents after inserting 100 small ones (should be 101) " + collection.count());

        // find first
        myDoc = collection.find().first();
        System.out.println(myDoc.toJson());

        // lets get all the documents in the collection and print them out
        MongoCursor<Document> cursor = collection.find().iterator();
        try {
            while (cursor.hasNext()) {
                System.out.println(cursor.next().toJson());
            }
        } finally {
            cursor.close();
        }

        // Force a few getMore ops
        FindIterable<Document> documents1 = collection.find().batchSize(10);
        for (Document d : documents1) {
            System.out.println(d);
        }

        for (Document cur : collection.find()) {
            System.out.println(cur.toJson());
        }

        // now use a query to get 1 document out
        myDoc = collection.find(eq("i", 71)).first();
        System.out.println(myDoc.toJson());

        // now use a range query to get a larger subset
        cursor = collection.find(gt("i", 50)).iterator();

        try {
            while (cursor.hasNext()) {
                System.out.println(cursor.next().toJson());
            }
        } finally {
            cursor.close();
        }

        // range query with multiple constraints
        cursor = collection.find(and(gt("i", 50), lte("i", 100))).iterator();

        try {
            while (cursor.hasNext()) {
                System.out.println(cursor.next().toJson());
            }
        } finally {
            cursor.close();
        }

        // Query Filters
        myDoc = collection.find(eq("i", 71)).first();
        System.out.println(myDoc.toJson());

        // now use a range query to get a larger subset
        Block<Document> printBlock = new Block<Document>() {
            @Override
            public void apply(final Document document) {
                System.out.println(document.toJson());
            }
        };
        collection.find(gt("i", 50)).forEach(printBlock);

        // filter where; 50 < i <= 100
        collection.find(and(gt("i", 50), lte("i", 100))).forEach(printBlock);

        // Sorting
        myDoc = collection.find(exists("i")).sort(descending("i")).first();
        System.out.println(myDoc.toJson());

        // Projection
        myDoc = collection.find().projection(excludeId()).first();
        System.out.println(myDoc.toJson());

        // Update One
        collection.updateOne(eq("i", 10), new Document("$set", new Document("i", 110)));

        // Update Many
        UpdateResult updateResult = collection.updateMany(lt("i", 100), new Document("$inc", new Document("i", 100)));
        System.out.println(updateResult.getModifiedCount());

        // Delete One
        collection.deleteOne(eq("i", 110));

        // Delete Many
        DeleteResult deleteResult = collection.deleteMany(gte("i", 100));
        System.out.println(deleteResult.getDeletedCount());

        collection.drop();

        // ordered bulk writes
        List<WriteModel<Document>> writes = new ArrayList<>();
        writes.add(new InsertOneModel<>(new Document("_id", 4)));
        writes.add(new InsertOneModel<>(new Document("_id", 5)));
        writes.add(new InsertOneModel<>(new Document("_id", 6)));
        writes.add(new UpdateOneModel<Document>(new Document("_id", 1), new Document("$set", new Document("x", 2))));
        writes.add(new DeleteOneModel<Document>(new Document("_id", 2)));
        writes.add(new ReplaceOneModel<>(new Document("_id", 3), new Document("_id", 3).append("x", 4)));

        collection.bulkWrite(writes);

        collection.drop();

        collection.bulkWrite(writes, new BulkWriteOptions().ordered(false));

        // Clean up
        database.drop();
    }
}