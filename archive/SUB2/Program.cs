using System;
using System.Collections.Generic;
using System.IO;

namespace SUB2
{
    /// <summary>
    /// SUB2 - Reads MONITORING.TXT, accepts a yyyyMMddHH time-window from console,
    /// and prints accuracy "&lt;correct&gt;/&lt;total&gt;" for requests whose P timestamp
    /// falls in that window (i.e. P.timestamp starts with the input).
    /// </summary>
    public class Program
    {
        public static void Main(string[] args)
        {
            const string monitoringFile = "MONITORING.TXT";

            // requestId -> (timestamp, value) for predicted rows
            Dictionary<string, KeyValuePair<string, int>> predicted =
                new Dictionary<string, KeyValuePair<string, int>>();
            Dictionary<string, int> actual = new Dictionary<string, int>();

            foreach (string raw in File.ReadLines(monitoringFile))
            {
                string line = raw.Trim();
                if (line.Length == 0) continue;

                string[] parts = line.Split('#');
                if (parts.Length != 4) continue;

                string requestId = parts[0];
                string timestamp = parts[1];
                string dataType = parts[2];
                int dataValue = int.Parse(parts[3]);

                if (dataType == "P")
                    predicted[requestId] = new KeyValuePair<string, int>(timestamp, dataValue);
                else if (dataType == "A")
                    actual[requestId] = dataValue;
            }

            string window = Console.ReadLine();
            if (window == null) window = string.Empty;
            window = window.Trim();

            int correct = 0;
            int total = 0;
            foreach (KeyValuePair<string, KeyValuePair<string, int>> kv in predicted)
            {
                if (!kv.Value.Key.StartsWith(window)) continue;
                total++;
                int actualValue;
                if (actual.TryGetValue(kv.Key, out actualValue) && kv.Value.Value == actualValue)
                    correct++;
            }

            Console.WriteLine(correct + "/" + total);
        }
    }
}
