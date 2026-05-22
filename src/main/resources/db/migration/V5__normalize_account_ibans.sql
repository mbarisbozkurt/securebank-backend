update accounts
set iban =
    'TR'
    || lpad(
        (
            98 - mod(
                (
                    '00062'
                    || lpad(id::text, 17, '0')
                    || '292700'
                )::numeric,
                97
            )
        )::text,
        2,
        '0'
    )
    || '00062'
    || lpad(id::text, 17, '0');
