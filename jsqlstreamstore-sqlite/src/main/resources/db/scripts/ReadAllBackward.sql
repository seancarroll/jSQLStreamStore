SELECT
	Streams.IdOriginal As StreamId,
	Messages.StreamVersion,
	Messages.Position,
	Messages.Id AS MessageId,
	Messages.Created,
	Messages.Type,
	Messages.JsonMetadata
FROM Messages
INNER JOIN Streams ON Messages.StreamIdInternal = Streams.IdInternal
WHERE Messages.Position <= ?
ORDER BY Messages.Position DESC
LIMIT ?;
