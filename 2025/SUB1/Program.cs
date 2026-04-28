using System;
using System.Collections.Generic;
using System.IO;

namespace SUB1
{
    /// <summary>
    /// SUB1 - Reads MONITORING.TXT and prints accuracy as "&lt;correct&gt;/&lt;total&gt;".
    /// File format per line: &lt;requestId&gt;#&lt;timestamp&gt;#&lt;dataType&gt;#&lt;dataValue&gt;
    ///   dataType: 'P' (predicted) or 'A' (actual). Same requestId has 1 P and 1 A row.
    /// Accuracy = (#requestIds where P value == A value) / (#requestIds)
    /// </summary>
    public class Program
    {
        public static void Main(string[] args)
        {
            const string monitoringFile = "MONITORING.TXT";

            Dictionary<string, int> predicted = new Dictionary<string, int>();
            Dictionary<string, int> actual = new Dictionary<string, int>();

            foreach (string raw in File.ReadLines(monitoringFile))
            {
                string line = raw.Trim();
                if (line.Length == 0) continue;

                string[] parts = line.Split('#');
                if (parts.Length != 4) continue;

                string requestId = parts[0];
                string dataType = parts[2];
                int dataValue = int.Parse(parts[3]);

                if (dataType == "P")
                    predicted[requestId] = dataValue;
                else if (dataType == "A")
                    actual[requestId] = dataValue;
            }

            int correct = 0;
            int total = predicted.Count;
            foreach (KeyValuePair<string, int> kv in predicted)
            {
                int actualValue;
                if (actual.TryGetValue(kv.Key, out actualValue) && kv.Value == actualValue)
                    correct++;
            }

            Console.WriteLine(correct + "/" + total);
        }
    }
}
